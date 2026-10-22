package ru.kino.dev.core

/** The cipher the content of a file is encrypted with. Twofish the core does not read. */
enum class Cipher { Aes256, ChaCha20 }

/** The Argon2 variant that turns the credentials into the key. AES-KDF is KDBX 3 - the core does not read it. */
enum class KdfVariant { Argon2id, Argon2d }

/**
 * What the settings change of the encryption of a database: the cipher, the Argon2 variant and its passes. Memory
 * and lanes stay as the file has them - memory is what makes guessing expensive, the screen does not lower it.
 */
data class Encryption(val cipher: Cipher, val kdf: KdfVariant, val iterations: Long)

/**
 * The security of an open database as its settings show it (pass-docs, Step 67).
 *
 * @param memoryBytes the Argon2 memory of the file, read only
 * @param parallelism the Argon2 lanes of the file, read only
 * @param keyFileName the name of the key file to show, when the database has one; never a path
 */
data class DatabaseSecurity(
    val encryption: Encryption,
    val memoryBytes: Long,
    val parallelism: Int,
    val credentials: CredentialsUsed,
    val keyFileName: String?,
)

/**
 * A change of the credentials of an open database - one at a time, what one dialog of the settings does.
 *
 * Not the four flags the core takes (`*_used` / `*_changed`): those are put together below, in the bridge, from
 * this and the credentials the database has now, so a combination that closes the database with something else
 * than meant cannot be written up here at all.
 */
sealed interface CredentialsChange {

    /** A new password, or the first one of a database with only a key file. */
    data class SetPassword(val password: String) : CredentialsChange {
        // Never the password: a change in a log is the password given away
        override fun toString(): String = "SetPassword(***)"
    }

    /** A new key file, or the first one. The holder wipes it once the call is done. */
    data class SetKeyFile(val keyFile: KeyFile) : CredentialsChange

    /** No key file any more - only when the database has a password; the core refuses both gone. */
    data object RemoveKeyFile : CredentialsChange
}
