package ru.kino.dev.settings.data.biometric

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.kino.dev.settings.biometric.BiometricCleanup
import ru.kino.dev.settings.biometric.BiometricFailure
import ru.kino.dev.settings.biometric.BiometricRepository
import ru.kino.dev.settings.biometric.BiometricSession
import kotlin.coroutines.cancellation.CancellationException

/**
 * [BiometricRepository] over a Keystore key and the store of the encrypted password.
 *
 * One password at a time, of the remembered database: turning biometrics on for a database, or
 * remembering another one, forgets what was there. The plain password exists only for the length of
 * [save] and [read]; its bytes are wiped right after.
 */
internal class BiometricRepositoryImpl(
    private val keys: BiometricKeys,
    private val store: BiometricPasswordStore,
) : BiometricRepository, BiometricCleanup {

    override fun isAvailable(): Boolean = keys.isAvailable()

    override suspend fun isEnabled(dbUri: String): Boolean = biometricCall { store.read()?.dbUri == dbUri }

    override suspend fun startSaving(): BiometricSession = biometricCall {
        store.clear()
        BiometricSession(keys.newEncryptCipher())
    }

    override suspend fun save(dbUri: String, password: String, session: BiometricSession) = biometricCall {
        val plain = password.toByteArray(Charsets.UTF_8)
        try {
            val ciphertext = session.cipher.doFinal(plain)
            store.write(StoredPassword(dbUri, session.cipher.iv, ciphertext))
        } finally {
            plain.fill(0)
        }
    }

    override suspend fun startReading(dbUri: String): BiometricSession = biometricCall {
        val stored = storedFor(dbUri)
        try {
            BiometricSession(keys.decryptCipher(stored.iv))
        } catch (e: BiometricFailure.KeyInvalidated) {
            // The key will never open again: what it encrypted is of no use
            forgetStoredAndKey()
            throw e
        }
    }

    override suspend fun read(dbUri: String, session: BiometricSession): String = biometricCall {
        val plain = session.cipher.doFinal(storedFor(dbUri).ciphertext)
        try {
            String(plain, Charsets.UTF_8)
        } finally {
            plain.fill(0)
        }
    }

    override suspend fun forget(dbUri: String) = biometricCall {
        if (store.read()?.dbUri == dbUri) forgetStoredAndKey()
    }

    override suspend fun forgetOtherThan(dbUri: String) = biometricCall {
        val stored = store.read() ?: return@biometricCall
        if (stored.dbUri != dbUri) forgetStoredAndKey()
    }

    private suspend fun storedFor(dbUri: String): StoredPassword =
        store.read()?.takeIf { it.dbUri == dbUri } ?: throw BiometricFailure.NotAvailable()

    override suspend fun forgetAll() = biometricCall { forgetStoredAndKey() }

    private suspend fun forgetStoredAndKey() {
        store.clear()
        keys.deleteKey()
    }

    // Keystore and data store block; whatever is not a BiometricFailure already is ours
    private suspend fun <T> biometricCall(block: suspend () -> T): T = withContext(Dispatchers.IO) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: BiometricFailure) {
            throw e
        } catch (e: Exception) {
            throw BiometricFailure.Unexpected(e)
        }
    }
}
