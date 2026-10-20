package ru.kino.dev.settings.pres.lock

import ru.kino.dev.core.CoreException
import ru.kino.dev.log.ErrorLog
import ru.kino.dev.vault.credentials.DatabaseCredentials

internal const val DB = "content://docs/passwords.kdbx"

/** The key of [DB]: [password], or none at all when [hasPassword] is false. */
internal class FakeDatabaseCredentials(
    private val hasPassword: Boolean = true,
    private val password: String = "pw",
) : DatabaseCredentials {

    /** Thrown by the next call, when set. */
    var failure: CoreException? = null

    override suspend fun hasPassword(dbKey: String): Boolean {
        failure?.let { throw it }
        return hasPassword
    }

    override suspend fun verifyPassword(dbKey: String, password: String): Boolean {
        failure?.let { throw it }
        return hasPassword && password == this.password
    }
}

internal class RecordingLog : ErrorLog {
    val logged = mutableListOf<Throwable>()

    override fun unexpected(where: String, error: Throwable) {
        logged += error
    }
}
