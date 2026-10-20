package ru.kino.dev.settings.biometric

import javax.crypto.Cipher

/**
 * Stored passwords by uri, in the clear - the Keystore and the prompt are not what processors test. Calls go to
 * [journal] as `"startSaving"`, `"save <uri>"`, `"forget <uri>"`.
 */
class FakeBiometrics(private val journal: MutableList<String> = mutableListOf()) : BiometricRepository {

    var available = true
    val passwords = mutableMapOf<String, String>()

    /** Thrown by the next [startReading], then forgotten. */
    var startFailure: BiometricFailure? = null

    override fun isAvailable() = available

    override suspend fun isEnabled(dbUri: String) = dbUri in passwords

    override suspend fun startSaving(): BiometricSession {
        journal += "startSaving"
        passwords.clear()
        return session()
    }

    override suspend fun save(dbUri: String, password: String, session: BiometricSession) {
        journal += "save $dbUri"
        passwords[dbUri] = password
    }

    override suspend fun startReading(dbUri: String): BiometricSession {
        startFailure?.let {
            startFailure = null
            if (it is BiometricFailure.KeyInvalidated) passwords.remove(dbUri)
            throw it
        }
        if (dbUri !in passwords) throw BiometricFailure.NotAvailable()
        return session()
    }

    override suspend fun read(dbUri: String, session: BiometricSession): String =
        passwords[dbUri] ?: throw BiometricFailure.NotAvailable()

    override suspend fun forget(dbUri: String) {
        journal += "forget $dbUri"
        passwords.remove(dbUri)
    }

    private fun session() = BiometricSession(Cipher.getInstance("AES/GCM/NoPadding"))
}
