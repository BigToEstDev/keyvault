package ru.kino.dev.welcome.pres

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import ru.kino.dev.log.ErrorLog
import ru.kino.dev.welcome.BiometricFailure
import ru.kino.dev.welcome.BiometricRepository
import ru.kino.dev.welcome.BiometricSession
import ru.kino.dev.welcome.CurrentDatabase
import ru.kino.dev.welcome.CurrentDatabaseRepository
import ru.kino.dev.welcome.PickedFile
import ru.kino.dev.welcome.WelcomeDatabaseRepository
import ru.kino.dev.welcome.WelcomeFailure
import ru.kino.dev.welcome.WelcomeFilesRepository
import javax.crypto.Cipher

/** Calls as `"<method> <uri>"`, shared by the fakes to check the order across them. */
internal typealias Journal = MutableList<String>

internal class FakeWelcomeDatabase(private val journal: Journal = mutableListOf()) : WelcomeDatabaseRepository {

    /** Thrown by the next call, then forgotten. */
    var failure: WelcomeFailure? = null

    /** When set, calls wait for it - to look at the state while the call is running. */
    var gate: CompletableDeferred<Unit>? = null

    var passwordReceived: String? = "unset"
    var keyFileReceived: String? = "unset"

    override suspend fun open(uri: String, password: String?, keyFileUri: String?): String {
        call("open", uri, password, keyFileUri)
        return uri
    }

    override suspend fun unlock(dbKey: String, password: String?, keyFileUri: String?) {
        call("unlock", dbKey, password, keyFileUri)
    }

    override suspend fun create(uri: String, password: String, keyFileUri: String?): String {
        call("create", uri, password, keyFileUri)
        return uri
    }

    private suspend fun call(method: String, uri: String, password: String?, keyFileUri: String?) {
        journal += "$method $uri"
        passwordReceived = password
        keyFileReceived = keyFileUri
        gate?.await()
        failure?.let {
            failure = null
            throw it
        }
    }
}

internal class FakeWelcomeFiles(private val journal: Journal = mutableListOf()) : WelcomeFilesRepository {

    val names = mutableMapOf<String, String>()

    /** Files that already have content - "save as" handed back an existing one. */
    val existing = mutableSetOf<String>()

    /** Thrown by the next [writeNewKeyFile], then forgotten. */
    var writeFailure: WelcomeFailure? = null

    /** Files deleted outside the app. */
    val missing = mutableSetOf<String>()

    override suspend fun displayName(uri: String): String? = names[uri]

    override suspend fun isReachable(uri: String): Boolean = uri !in missing

    override suspend fun wouldOverwrite(uri: String, file: PickedFile): Boolean {
        journal += "wouldOverwrite $uri"
        return uri in existing
    }

    override suspend fun writeNewKeyFile(uri: String) {
        journal += "writeNewKeyFile $uri"
        writeFailure?.let {
            writeFailure = null
            throw it
        }
    }
}

internal class FakeCurrentDatabase(database: CurrentDatabase? = null) : CurrentDatabaseRepository {
    override val current = MutableStateFlow(database)
}

/** Stored passwords by uri, in the clear - the Keystore and the prompt are not what processors test. */
internal class FakeBiometrics(private val journal: Journal = mutableListOf()) : BiometricRepository {

    var available = true
    val passwords = mutableMapOf<String, String>()

    /** Thrown by the next [startReading], then forgotten. */
    var startFailure: BiometricFailure? = null

    override fun isAvailable() = available

    override suspend fun isEnabled(dbUri: String) = dbUri in passwords

    override suspend fun startSaving(): BiometricSession {
        journal += "startSaving"
        passwords.clear()
        return session()
    }

    override suspend fun save(dbUri: String, password: String, session: BiometricSession) {
        journal += "save $dbUri"
        passwords[dbUri] = password
    }

    override suspend fun startReading(dbUri: String): BiometricSession {
        startFailure?.let {
            startFailure = null
            if (it is BiometricFailure.KeyInvalidated) passwords.remove(dbUri)
            throw it
        }
        if (dbUri !in passwords) throw BiometricFailure.NotAvailable()
        return session()
    }

    override suspend fun read(dbUri: String, session: BiometricSession): String =
        passwords[dbUri] ?: throw BiometricFailure.NotAvailable()

    override suspend fun forget(dbUri: String) {
        journal += "forget $dbUri"
        passwords.remove(dbUri)
    }

    private fun session() = BiometricSession(Cipher.getInstance("AES/GCM/NoPadding"))
}

internal class FakeErrorLog : ErrorLog {
    val logged = mutableListOf<Throwable>()

    override fun unexpected(where: String, error: Throwable) {
        logged += error
    }
}
