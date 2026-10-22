package ru.kino.dev.vault.security

import ru.kino.dev.core.CredentialsChange
import ru.kino.dev.core.DatabaseSecurity
import ru.kino.dev.core.Encryption
import ru.kino.dev.core.KeyFile
import kotlin.time.Duration

/**
 * The security of the open database - the cipher, the key derivation and the credentials (pass-docs, Step 67). Of
 * `vault`: the database is open, and its file is written by `vault`. Changes go through [SecurityEditor], which
 * writes the file.
 *
 * Throws [SecurityFailure] and nothing else.
 */
interface SecurityRepository {

    /** The security as the database has it now. */
    suspend fun read(dbKey: String): DatabaseSecurity

    /** Sets the encryption - in memory only, the file is not written. */
    suspend fun changeEncryption(dbKey: String, encryption: Encryption)

    /** Applies [change] to the credentials - in memory only, the old file opens with the old ones. */
    suspend fun changeCredentials(dbKey: String, change: CredentialsChange)

    /** Whether [password] is the password of the database - checked alone, at once. False without a password. */
    suspend fun verifyPassword(dbKey: String, password: String): Boolean

    /** How long one KDF pass of the database takes on this device; about as long as a pass. */
    suspend fun measureKdfPass(dbKey: String): Duration

    /**
     * Reads the key file at [uri] the person picked - its size checked before it is in memory - and keeps access to
     * it for the sign-in screens, which read it anew on every opening. The caller wipes the result.
     */
    suspend fun readKeyFile(uri: String): KeyFile
}

/** Every way reading or changing the security can fail, as the screen answers it. */
sealed class SecurityFailure(message: String, cause: Throwable? = null) : Exception(message, cause) {

    /** The current password given is not the password of the database. Nothing changed. */
    class WrongPassword : SecurityFailure("The current password does not match")

    /** The key file is the only credential left - it cannot go. Nothing changed. */
    class LastFactor : SecurityFailure("The key file is the only credential")

    /** The key file picked is over the limit. Nothing changed. */
    class KeyFileTooLarge(cause: Throwable? = null) : SecurityFailure("The key file is too large", cause)

    /** The key file picked could not be read. Nothing changed. */
    class KeyFileUnreadable(cause: Throwable? = null) : SecurityFailure("The key file cannot be read", cause)

    /** The change is made in the database, but writing the file failed: it waits in memory for the next save. */
    class NotSaved(cause: Throwable? = null) : SecurityFailure("The change is made but not written to the file", cause)

    /**
     * The key file is changed and saved, but the sign-in screens still remember the old one: on the next opening
     * the new key file has to be picked there.
     */
    class KeyFileNotRemembered(cause: Throwable? = null) :
        SecurityFailure("The key file is changed but the old one is remembered", cause)

    /** The core refused - the database closed meanwhile, or a bug. The cause to the log. */
    class Unexpected(cause: Throwable? = null) : SecurityFailure("Something went wrong", cause)
}
