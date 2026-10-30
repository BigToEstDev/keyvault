package ru.kino.dev.settings.biometric

import javax.crypto.Cipher

/**
 * The password of the remembered database, kept for unlocking it with strong biometrics on this phone.
 *
 * A second way in, never the only one: the master password always works, and the database itself knows
 * nothing of this. The password is stored encrypted by a key of the Android Keystore that opens only after
 * the user's biometrics - each use asks again, there is no window of validity (pass-docs, Step 33).
 *
 * Every use is two calls around the system prompt: `start...` hands out a [BiometricSession], the screen
 * shows the prompt with its cipher, and the call after it works only with a session the prompt let
 * through.
 *
 * Failures are [BiometricFailure]. Of `settings`, since Step 59: `welcome` signs in with it, the settings turn it on
 * and off.
 */
interface BiometricRepository {

    /** Whether the platform has biometrics at all - none on the desktop yet (pass-docs, Step 90). */
    fun isSupported(): Boolean

    /** Whether the phone has strong biometrics set up - without them nothing here is offered. */
    fun isAvailable(): Boolean

    /** Whether the password of [dbUri] is stored for biometrics. */
    suspend fun isEnabled(dbUri: String): Boolean

    /**
     * Begins storing a password: a fresh key, and a session to encrypt with. Whatever was stored before -
     * for this database or another - is gone.
     */
    suspend fun startSaving(): BiometricSession

    /** Stores [password] for [dbUri], encrypted with the session the prompt let through. */
    suspend fun save(dbUri: String, password: String, session: BiometricSession)

    /**
     * Begins reading the password of [dbUri]: a session to decrypt with.
     *
     * When the biometrics of the phone changed since the password was stored, the key no longer opens:
     * the stored password is forgotten and this fails with [BiometricFailure.KeyInvalidated].
     */
    suspend fun startReading(dbUri: String): BiometricSession

    /** The password of [dbUri], decrypted with the session the prompt let through. */
    suspend fun read(dbUri: String, session: BiometricSession): String

    /**
     * Forgets the stored password of [dbUri] and its key. Nothing is stored afterwards, so there is nothing
     * to unlock with until biometrics are turned on again.
     *
     * Called when the stored password no longer opens the database, and - by contract - by the settings
     * when they change or remove the password of the database (pass-docs, `welcome/decisions.md`,
     * "Безопасность").
     */
    suspend fun forget(dbUri: String)
}

/**
 * A cipher waiting for the user's biometrics. The screen hands [cipher] to the system prompt; after the
 * prompt let it through, the session goes back to the repository. Holds nothing secret by itself.
 */
class BiometricSession(val cipher: Cipher)

/** How using biometrics can fail. The message is for the log. */
sealed class BiometricFailure(message: String, cause: Throwable? = null) : Exception(message, cause) {

    /**
     * The biometrics of the phone changed - a fingerprint added or removed - and the key is gone with them.
     * The stored password is forgotten already; the screen says why and asks for the password.
     */
    class KeyInvalidated(cause: Throwable? = null) :
        BiometricFailure("Biometrics of the device changed, the key is invalidated", cause)

    /** Nothing is stored for this database, or the phone has no strong biometrics. */
    class NotAvailable : BiometricFailure("Biometric unlock is not available")

    /** Ours: a bug, or a state that should not happen. */
    class Unexpected(cause: Throwable? = null) : BiometricFailure("Biometric unlock failed", cause)
}
