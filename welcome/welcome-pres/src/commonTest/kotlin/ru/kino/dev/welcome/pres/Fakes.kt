package ru.kino.dev.welcome.pres

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import ru.kino.dev.log.ErrorLog
import ru.kino.dev.welcome.CurrentDatabase
import ru.kino.dev.welcome.CurrentDatabaseRepository
import ru.kino.dev.welcome.PickedFile
import ru.kino.dev.welcome.WelcomeDatabaseRepository
import ru.kino.dev.welcome.WelcomeFailure
import ru.kino.dev.welcome.WelcomeFilesRepository

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

    /** Thrown by the next [checkKeyFile], then forgotten. */
    var checkFailure: WelcomeFailure? = null

    override suspend fun checkKeyFile(uri: String) {
        journal += "checkKeyFile $uri"
        checkFailure?.let {
            checkFailure = null
            throw it
        }
    }

    override suspend fun writeNewKeyFile(uri: String) {
        journal += "writeNewKeyFile $uri"
        writeFailure?.let {
            writeFailure = null
            throw it
        }
    }

    override suspend fun discardEmpty(uri: String) {
        journal += "discardEmpty $uri"
    }
}

internal class FakeCurrentDatabase(database: CurrentDatabase? = null) : CurrentDatabaseRepository {
    override val current = MutableStateFlow(database)
}

internal class FakeErrorLog : ErrorLog {
    val logged = mutableListOf<Throwable>()

    override fun unexpected(where: String, error: Throwable) {
        logged += error
    }
}
