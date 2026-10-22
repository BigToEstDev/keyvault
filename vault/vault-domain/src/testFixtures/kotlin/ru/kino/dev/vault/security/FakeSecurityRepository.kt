package ru.kino.dev.vault.security

import ru.kino.dev.core.Cipher
import ru.kino.dev.core.CredentialsChange
import ru.kino.dev.core.CredentialsUsed
import ru.kino.dev.core.DatabaseSecurity
import ru.kino.dev.core.Encryption
import ru.kino.dev.core.KdfVariant
import ru.kino.dev.core.KeyFile
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * The security in memory: what [read] answers, changed the way the core would. [failure], when set, is thrown by
 * every call instead; [keyFileFailure] - by [readKeyFile] alone.
 */
class FakeSecurityRepository(
    var security: DatabaseSecurity = DatabaseSecurity(
        encryption = Encryption(Cipher.Aes256, KdfVariant.Argon2id, iterations = 3),
        memoryBytes = 64L * 1024 * 1024,
        parallelism = 2,
        credentials = CredentialsUsed(password = true, keyFile = false),
        keyFileName = null,
    ),
) : SecurityRepository {

    var failure: SecurityFailure? = null
    var keyFileFailure: SecurityFailure? = null

    /** The password [verifyPassword] takes. */
    var password = "current password"

    var kdfPass: Duration = 850.milliseconds

    /** Every encryption asked for, in order. */
    val encryptions = mutableListOf<Encryption>()

    /** Every change of the credentials asked for, in order. */
    val credentialChanges = mutableListOf<CredentialsChange>()

    /** Every key file handed out by [readKeyFile], as the same instances - a test checks they were wiped. */
    val keyFilesRead = mutableListOf<KeyFile>()

    override suspend fun read(dbKey: String): DatabaseSecurity {
        failure?.let { throw it }
        return security
    }

    override suspend fun changeEncryption(dbKey: String, encryption: Encryption) {
        failure?.let { throw it }
        encryptions += encryption
        security = security.copy(encryption = encryption)
    }

    override suspend fun changeCredentials(dbKey: String, change: CredentialsChange) {
        failure?.let { throw it }
        credentialChanges += change
        security = when (change) {
            is CredentialsChange.SetPassword -> {
                password = change.password
                security.copy(credentials = security.credentials.copy(password = true))
            }
            is CredentialsChange.SetKeyFile ->
                security.copy(credentials = security.credentials.copy(keyFile = true), keyFileName = change.keyFile.name)
            CredentialsChange.RemoveKeyFile ->
                security.copy(credentials = security.credentials.copy(keyFile = false), keyFileName = null)
        }
    }

    override suspend fun verifyPassword(dbKey: String, password: String): Boolean {
        failure?.let { throw it }
        return security.credentials.password && password == this.password
    }

    override suspend fun measureKdfPass(dbKey: String): Duration {
        failure?.let { throw it }
        return kdfPass
    }

    override suspend fun readKeyFile(uri: String): KeyFile {
        failure?.let { throw it }
        keyFileFailure?.let { throw it }
        return KeyFile(name = uri.substringAfterLast('/'), content = byteArrayOf(1, 2, 3)).also { keyFilesRead += it }
    }
}
