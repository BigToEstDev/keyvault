package ru.kino.dev.settings.biometric

/**
 * Forgetting the biometrics of a database that is no longer the remembered one - used by the repository of
 * `welcome` that remembers databases. Apart from [BiometricRepository]: the screens never forget everything.
 */
interface BiometricCleanup {

    /** Forgets what is stored unless it belongs to [dbUri]. */
    suspend fun forgetOtherThan(dbUri: String)

    /** Forgets what is stored, whichever database it belongs to - a new database was created. */
    suspend fun forgetAll()
}
