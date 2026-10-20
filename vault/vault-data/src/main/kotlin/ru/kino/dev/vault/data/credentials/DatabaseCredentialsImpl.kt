package ru.kino.dev.vault.data.credentials

import ru.kino.dev.core.NativeCore
import ru.kino.dev.vault.credentials.DatabaseCredentials
import javax.inject.Inject

/** [DatabaseCredentials] on the core: `credentials_used` and `verify_password` (pass-docs, Step 60). */
internal class DatabaseCredentialsImpl @Inject constructor(
    private val core: NativeCore,
) : DatabaseCredentials {

    override suspend fun hasPassword(dbKey: String): Boolean = core.credentialsUsed(dbKey).password

    override suspend fun verifyPassword(dbKey: String, password: String): Boolean =
        core.verifyPassword(dbKey, password)
}
