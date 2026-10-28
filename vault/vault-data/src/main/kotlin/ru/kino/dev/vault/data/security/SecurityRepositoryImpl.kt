package ru.kino.dev.vault.data.security

import ru.kino.dev.core.CoreException
import ru.kino.dev.core.CredentialLimits
import ru.kino.dev.core.CredentialsChange
import ru.kino.dev.core.DatabaseFileException
import ru.kino.dev.core.DatabaseFiles
import ru.kino.dev.core.DatabaseSecurity
import ru.kino.dev.core.Encryption
import ru.kino.dev.core.KeyFile
import ru.kino.dev.core.NativeCore
import ru.kino.dev.core.readKeyFile
import ru.kino.dev.vault.security.SecurityFailure
import ru.kino.dev.vault.security.SecurityRepository
import kotlin.time.Duration

/**
 * [SecurityRepository] on the core - `get_db_settings` / `set_db_settings`, `verify_password`, `measure_kdf_pass` -
 * and on the documents for the key file (pass-docs, Step 67).
 */
internal class SecurityRepositoryImpl(
    private val core: NativeCore,
    private val documents: DatabaseFiles,
) : SecurityRepository {

    override suspend fun read(dbKey: String): DatabaseSecurity = coreCall { core.databaseSecurity(dbKey) }

    override suspend fun changeEncryption(dbKey: String, encryption: Encryption) {
        coreCall { core.changeDatabaseEncryption(dbKey, encryption) }
    }

    override suspend fun changeCredentials(dbKey: String, change: CredentialsChange) {
        coreCall { core.changeDatabaseCredentials(dbKey, change) }
    }

    override suspend fun verifyPassword(dbKey: String, password: String): Boolean =
        coreCall { core.verifyPassword(dbKey, password) }

    override suspend fun measureKdfPass(dbKey: String): Duration = coreCall { core.measureKdfPass(dbKey) }

    override suspend fun readKeyFile(uri: String): KeyFile {
        val keyFile = try {
            documents.readKeyFile(uri)
        } catch (e: CoreException) {
            throw if (e.kind == CredentialLimits.KIND_KEY_FILE_TOO_LARGE) {
                SecurityFailure.KeyFileTooLarge(e)
            } else {
                SecurityFailure.Unexpected(e)
            }
        } catch (e: DatabaseFileException) {
            throw SecurityFailure.KeyFileUnreadable(e)
        }
        try {
            // The sign-in screens read it again on every opening, after the app was restarted too
            documents.keepAccess(uri, forWriting = false)
        } catch (e: DatabaseFileException) {
            keyFile.wipe()
            throw SecurityFailure.KeyFileUnreadable(e)
        }
        return keyFile
    }

    private inline fun <T> coreCall(block: () -> T): T = try {
        block()
    } catch (e: CoreException) {
        throw SecurityFailure.Unexpected(e)
    }
}
