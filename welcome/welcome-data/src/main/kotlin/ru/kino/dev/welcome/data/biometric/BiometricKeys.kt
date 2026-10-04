package ru.kino.dev.welcome.data.biometric

import javax.crypto.Cipher

/**
 * The key that encrypts the stored password, and ciphers on it. Kept behind an interface so that the logic
 * around it runs in tests on the jvm; the real one is [KeystoreBiometricKeys].
 */
internal interface BiometricKeys {

    /** Whether the phone has strong biometrics set up. */
    fun isAvailable(): Boolean

    /** Replaces the key with a fresh one and returns a cipher to encrypt with - not usable before the prompt. */
    fun newEncryptCipher(): Cipher

    /**
     * A cipher to decrypt with, for what was encrypted with [iv].
     *
     * @throws ru.kino.dev.welcome.BiometricFailure.KeyInvalidated when the key is gone or invalidated by a
     * change of the biometrics of the phone
     */
    fun decryptCipher(iv: ByteArray): Cipher

    /** Deletes the key; nothing it encrypted can be read afterwards. */
    fun deleteKey()
}

/** The password as it is stored: encrypted, with the iv it needs, and the database it belongs to. */
internal class StoredPassword(val dbUri: String, val iv: ByteArray, val ciphertext: ByteArray)

/** Where [StoredPassword] lives between launches. */
internal interface BiometricPasswordStore {

    suspend fun read(): StoredPassword?

    suspend fun write(password: StoredPassword)

    suspend fun clear()
}

/**
 * Forgetting the biometrics of a database that is no longer the remembered one - used by the repository that
 * remembers databases.
 */
internal interface BiometricCleanup {

    /** Forgets what is stored unless it belongs to [dbUri]. */
    suspend fun forgetOtherThan(dbUri: String)
}
