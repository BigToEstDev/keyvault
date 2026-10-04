package ru.kino.dev.welcome.data.biometric

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.kino.dev.core.expectFailure
import ru.kino.dev.welcome.BiometricFailure
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class BiometricRepositoryImplTest {

    private val keys = SoftwareKeys()
    private val store = MemoryStore()
    private val repository = BiometricRepositoryImpl(keys, store)

    @Test
    fun `a password saved through one session reads back through another`() = runTest {
        enable(DB, "secret")

        assertTrue(repository.isEnabled(DB))
        val session = repository.startReading(DB)
        assertEquals("secret", repository.read(DB, session))
    }

    @Test
    fun `what is stored is ciphertext, not the password`() = runTest {
        enable(DB, "secret")

        val stored = requireNotNull(store.stored)
        assertFalse(String(stored.ciphertext, Charsets.UTF_8).contains("secret"))
    }

    @Test
    fun `turning biometrics on again replaces the key - the old password is gone`() = runTest {
        enable(DB, "secret")
        repository.startSaving()

        assertNull(store.stored)
        assertFalse(repository.isEnabled(DB))
    }

    @Test
    fun `an invalidated key forgets the stored password and says why`() = runTest {
        enable(DB, "secret")
        keys.invalidated = true

        expectFailure<BiometricFailure.KeyInvalidated> { repository.startReading(DB) }

        assertNull(store.stored)
        assertEquals(1, keys.deleted)
    }

    @Test
    fun `nothing stored for this database is not available`() = runTest {
        enable(OTHER, "secret")

        assertFalse(repository.isEnabled(DB))
        expectFailure<BiometricFailure.NotAvailable> { repository.startReading(DB) }
    }

    @Test
    fun `forgetting a database forgets its password and key, not another one`() = runTest {
        enable(DB, "secret")

        repository.forget(OTHER)
        assertTrue(repository.isEnabled(DB))

        repository.forget(DB)
        assertFalse(repository.isEnabled(DB))
        assertEquals(1, keys.deleted)
    }

    @Test
    fun `remembering another database forgets the biometrics of the previous one`() = runTest {
        enable(DB, "secret")

        repository.forgetOtherThan(DB)
        assertTrue(repository.isEnabled(DB))

        repository.forgetOtherThan(OTHER)
        assertFalse(repository.isEnabled(DB))
    }

    @Test
    fun `a session the prompt did not let through is ours, not a password`() = runTest {
        enable(DB, "secret")
        keys.lockedUntilPrompt = true
        val session = repository.startReading(DB)

        expectFailure<BiometricFailure.Unexpected> { repository.read(DB, session) }
    }

    private suspend fun enable(dbUri: String, password: String) {
        val session = repository.startSaving()
        repository.save(dbUri, password, session)
    }

    /** The real AES/GCM of the jvm in place of the Keystore. */
    private class SoftwareKeys : BiometricKeys {
        var key: SecretKey? = null
        var invalidated = false
        var deleted = 0

        /** The next decrypt cipher is initialised for a wrong iv, the way a cipher nobody unlocked fails. */
        var lockedUntilPrompt = false

        override fun isAvailable() = true

        override fun newEncryptCipher(): Cipher {
            key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
            return Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key) }
        }

        override fun decryptCipher(iv: ByteArray): Cipher {
            val key = key?.takeUnless { invalidated } ?: throw BiometricFailure.KeyInvalidated()
            val usedIv = if (lockedUntilPrompt) ByteArray(iv.size) else iv
            return Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, usedIv))
            }
        }

        override fun deleteKey() {
            key = null
            deleted++
        }
    }

    private class MemoryStore : BiometricPasswordStore {
        var stored: StoredPassword? = null

        override suspend fun read() = stored

        override suspend fun write(password: StoredPassword) {
            stored = password
        }

        override suspend fun clear() {
            stored = null
        }
    }

    private companion object {
        const val DB = "content://docs/passwords.kdbx"
        const val OTHER = "content://docs/other.kdbx"
    }
}
