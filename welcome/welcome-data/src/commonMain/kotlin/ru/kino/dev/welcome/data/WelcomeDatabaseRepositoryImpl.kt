package ru.kino.dev.welcome.data

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import ru.kino.dev.core.DatabaseFiles
import ru.kino.dev.core.KeyFile
import ru.kino.dev.core.NativeCore
import ru.kino.dev.core.NewDatabase
import ru.kino.dev.core.OpenedDatabase
import ru.kino.dev.core.readKeyFile
import ru.kino.dev.settings.biometric.BiometricCleanup
import ru.kino.dev.settings.biometric.BiometricFailure
import ru.kino.dev.welcome.CurrentDatabase
import ru.kino.dev.welcome.CurrentDatabaseRepository
import ru.kino.dev.welcome.PickedFile
import ru.kino.dev.welcome.WelcomeDatabaseRepository

/**
 * [WelcomeDatabaseRepository] over the native core, the picked files and the remembered database.
 *
 * The order is the point of this class. A database is handed up only when everything is fixed: open in the
 * core, access to its files kept, remembered - otherwise "the database is open, but we did not remember
 * it". Each step that fails undoes what came before it: the core closes what it opened, an empty file made
 * for a new database is deleted.
 *
 * The content of a key file is read right before the call that needs it and wiped right after, whatever
 * the call ended with.
 */
internal class WelcomeDatabaseRepositoryImpl(
    private val core: NativeCore,
    private val files: DatabaseFiles,
    private val current: CurrentDatabaseRepository,
    private val writer: CurrentDatabaseWriter,
    private val biometrics: BiometricCleanup,
) : WelcomeDatabaseRepository {

    override suspend fun open(uri: String, password: String?, keyFileUri: String?): String = welcomeCall {
        val bytes = accessing(PickedFile.Database) { files.read(uri) }
        val fileName = files.displayName(uri)
        val keyFile = keyFileUri?.let { readKeyFile(it) }

        val opened = keyFile.wipedAfter {
            core.openDatabase(dbKey = uri, bytes = bytes, password = password, fileName = fileName, keyFile = it)
        }
        fix(opened, fileName, keyFileUri, keyFile?.name, created = false)
    }

    override suspend fun unlock(dbKey: String, password: String?, keyFileUri: String?) = welcomeCall {
        val keyFile = keyFileUri?.let { readKeyFile(it) }
        val keyFileName = keyFile?.name

        keyFile.wipedAfter { core.unlockDatabase(dbKey = dbKey, password = password, keyFile = it) }
        rememberKeyFile(dbKey, keyFileUri, keyFileName)
    }

    /**
     * A key file other than the remembered one unlocked the database: it replaced one that could not be
     * reached, and the next launch has to open with it. Fixed the way opening fixes it, or the database is
     * locked again - unlocked with a key file the app does not remember, it would not open after a restart.
     */
    private suspend fun rememberKeyFile(dbKey: String, keyFileUri: String?, keyFileName: String?) {
        val remembered = current.current.first()?.takeIf { it.uri == dbKey } ?: return
        if (remembered.keyFileUri == keyFileUri) return

        undoingOnFailure(undo = { core.lockDatabase(dbKey) }) {
            keyFileUri?.let { accessing(PickedFile.KeyFile) { files.keepAccess(it, forWriting = false) } }
            writer.remember(remembered.copy(keyFileUri = keyFileUri, keyFileName = keyFileName))
        }
    }

    override suspend fun create(uri: String, password: String, keyFileUri: String?): String = welcomeCall {
        // Asked before anything else: a file that was empty is ours to delete if creating fails. One that
        // was not is a file the user chose to write over - deleting it would lose what is still in it
        val wasEmpty = accessing(PickedFile.Database) { files.isEmpty(uri) }
        try {
            createIn(uri, password, keyFileUri)
        } catch (e: Throwable) {
            // Cancellation included: the user left the screen, and an abandoned creation leaves nothing.
            // Best effort - a provider that cannot delete leaves the file, and the failure above stays the
            // one reported
            if (wasEmpty) withContext(NonCancellable) { files.delete(uri) }
            throw e
        }
    }

    private suspend fun createIn(uri: String, password: String, keyFileUri: String?): String {
        val fileName = files.displayName(uri)
        val keyFile = keyFileUri?.let { readKeyFile(it) }

        val created = keyFile.wipedAfter {
            core.createDatabase(
                NewDatabase(
                    dbKey = uri,
                    fileName = fileName,
                    databaseName = databaseNameOf(fileName),
                    password = password,
                    keyFile = it,
                ),
            )
        }

        closingOnFailure(uri) {
            // Cancelling halfway through the write would leave a truncated database behind
            withContext(NonCancellable) {
                accessing(PickedFile.Database) { files.writeReplacing(uri, created.bytes) }
            }
        }
        return fix(created.database, fileName, keyFileUri, keyFile?.name, created = true)
    }

    /**
     * Keeps access to the files and remembers the database - the steps after which it may be handed up.
     *
     * Access that cannot be kept is "no access", and the database is closed again: remembered without it,
     * it would not open after a restart.
     *
     * @param created a new database - the password stored for biometrics is of no database here any more,
     * even one at the same uri
     */
    private suspend fun fix(
        opened: OpenedDatabase,
        fileName: String?,
        keyFileUri: String?,
        keyFileName: String?,
        created: Boolean,
    ): String {
        closingOnFailure(opened.dbKey) {
            accessing(PickedFile.Database) { files.keepAccess(opened.dbKey, forWriting = true) }
            keyFileUri?.let { accessing(PickedFile.KeyFile) { files.keepAccess(it, forWriting = false) } }

            writer.remember(
                CurrentDatabase(
                    uri = opened.dbKey,
                    // The name the app resolved: the core has none for a uri - see OpenedDatabase
                    fileName = fileName,
                    databaseName = opened.databaseName,
                    keyFileUri = keyFileUri,
                    keyFileName = keyFileName,
                ),
            )
        }
        forgetStaleBiometrics(opened.dbKey, created)
        return opened.dbKey
    }

    /**
     * The password stored for biometrics belongs to the remembered database. Opening another one makes it
     * stale; creating one makes it stale whatever the uri - a database deleted and created anew in the same
     * place has the same uri, and its old password must not unlock the new one (pass-docs, Step 34, bug 3).
     *
     * Best effort, so that it does not fail an opening or a creation that has succeeded: a stored password
     * left behind is caught at the next biometric sign-in - it does not open, and is forgotten then.
     */
    private suspend fun forgetStaleBiometrics(dbUri: String, created: Boolean) {
        try {
            if (created) biometrics.forgetAll() else biometrics.forgetOtherThan(dbUri)
        } catch (e: BiometricFailure) {
            // Left behind, caught at sign-in
        }
    }

    private suspend fun <T> closingOnFailure(dbKey: String, block: suspend () -> T): T =
        undoingOnFailure(undo = { core.closeDatabase(dbKey) }, block = block)

    private suspend fun <T> undoingOnFailure(undo: suspend () -> Unit, block: suspend () -> T): T =
        try {
            block()
        } catch (e: Throwable) {
            // The failure that brought us here is the one to report; one of undoing goes along with it
            withContext(NonCancellable) {
                runCatching { undo() }.exceptionOrNull()?.let(e::addSuppressed)
            }
            throw e
        }

    private suspend fun readKeyFile(uri: String): KeyFile = accessing(PickedFile.KeyFile) { files.readKeyFile(uri) }

    // The content is a secret on par with the password: gone as soon as the core has taken it
    private inline fun <T> KeyFile?.wipedAfter(block: (KeyFile?) -> T): T =
        try {
            block(this)
        } finally {
            this?.wipe()
        }

    // The file name without the extension; a provider that tells no name, or a file called just ".kdbx",
    // gets the default rather than a database with no name to show
    private fun databaseNameOf(fileName: String?): String =
        fileName
            ?.let { if (it.endsWith(KDBX, ignoreCase = true)) it.dropLast(KDBX.length) else it }
            ?.takeIf { it.isNotBlank() }
            ?: DEFAULT_DATABASE_NAME

    private companion object {
        const val KDBX = ".kdbx"

        /** Name of a new database whose file has no name to take it from. */
        const val DEFAULT_DATABASE_NAME = "kdbxvault"
    }
}
