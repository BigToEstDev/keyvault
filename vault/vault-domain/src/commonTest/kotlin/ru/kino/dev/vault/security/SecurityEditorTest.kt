package ru.kino.dev.vault.security

import kotlinx.coroutines.test.runTest
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.Test
import ru.kino.dev.core.Cipher
import ru.kino.dev.core.CredentialsChange
import ru.kino.dev.core.CredentialsUsed
import ru.kino.dev.core.Encryption
import ru.kino.dev.core.KdfVariant
import ru.kino.dev.core.expectFailure
import ru.kino.dev.settings.biometric.FakeBiometrics
import ru.kino.dev.settings.signin.FakeRememberedKeyFile
import ru.kino.dev.vault.FakeVaultDatabase
import java.io.IOException

class SecurityEditorTest {

    private val journal = mutableListOf<String>()
    private val security = FakeSecurityRepository()
    private val database = FakeVaultDatabase()
    private val biometrics = FakeBiometrics(journal).apply { passwords[DB] = "current password" }
    private val remembered = FakeRememberedKeyFile(journal)
    private val editor = SecurityEditor(security, database, biometrics, remembered)

    // --- Encryption ---

    @Test
    fun `the encryption chosen is set and saved`() = runTest {
        val chosen = Encryption(Cipher.ChaCha20, KdfVariant.Argon2d, iterations = 5)

        editor.setEncryption(DB, chosen)

        assertEquals(listOf(chosen), security.encryptions)
        assertEquals(listOf("save"), database.calls)
    }

    @Test
    fun `the same encryption is not a change - nothing is written`() = runTest {
        editor.setEncryption(DB, security.security.encryption)

        assertEquals(emptyList<Encryption>(), security.encryptions)
        assertEquals(emptyList<String>(), database.calls)
    }

    @Test
    fun `an encryption not written is not saved, and stays in memory`() = runTest {
        database.saveFailure = IOException("provider gone")

        expectFailure<SecurityFailure.NotSaved> {
            editor.setEncryption(DB, Encryption(Cipher.ChaCha20, KdfVariant.Argon2id, iterations = 3))
        }
        assertEquals(Cipher.ChaCha20, security.security.encryption.cipher)
    }

    // --- Password ---

    @Test
    fun `a new password needs the current one, saves and forgets the biometrics`() = runTest {
        editor.setPassword(DB, current = "current password", new = NEW_PASSWORD)

        assertEquals(listOf<CredentialsChange>(CredentialsChange.SetPassword(NEW_PASSWORD)), security.credentialChanges)
        assertEquals(listOf("save"), database.calls)
        assertEquals(listOf("forget $DB"), journal)
    }

    @Test
    fun `a wrong current password changes nothing`() = runTest {
        expectFailure<SecurityFailure.WrongPassword> {
            editor.setPassword(DB, current = "not it", new = NEW_PASSWORD)
        }

        assertEquals(emptyList<CredentialsChange>(), security.credentialChanges)
        assertEquals(emptyList<String>(), database.calls)
        assertEquals(emptyList<String>(), journal)
    }

    @Test
    fun `no current password for a database with one changes nothing`() = runTest {
        expectFailure<SecurityFailure.WrongPassword> { editor.setPassword(DB, current = null, new = NEW_PASSWORD) }

        assertEquals(emptyList<CredentialsChange>(), security.credentialChanges)
    }

    @Test
    fun `a database with only a key file gets its first password without a current one`() = runTest {
        security.security = security.security.copy(credentials = CredentialsUsed(password = false, keyFile = true))

        editor.setPassword(DB, current = null, new = NEW_PASSWORD)

        assertEquals(listOf<CredentialsChange>(CredentialsChange.SetPassword(NEW_PASSWORD)), security.credentialChanges)
        assertEquals(listOf("save"), database.calls)
    }

    // The password is changed in memory - an unlock goes with it - so the stored one is stale all the same
    @Test
    fun `a new password not written still forgets the biometrics`() = runTest {
        database.saveFailure = IOException("provider gone")

        expectFailure<SecurityFailure.NotSaved> {
            editor.setPassword(DB, current = "current password", new = NEW_PASSWORD)
        }
        assertEquals(listOf("forget $DB"), journal)
    }

    // --- Key file ---

    @Test
    fun `a key file is set, saved, remembered and wiped`() = runTest {
        editor.setKeyFile(DB, KEY)

        val keyFile = security.keyFilesRead.single()
        assertEquals(listOf<CredentialsChange>(CredentialsChange.SetKeyFile(keyFile)), security.credentialChanges)
        assertEquals(listOf("save"), database.calls)
        assertEquals(listOf("remember $DB $KEY"), journal)
        assertEquals("home.keyx", remembered.keyFileName)
        assertTrue(keyFile.content.all { it == 0.toByte() })
    }

    @Test
    fun `a key file not read changes nothing`() = runTest {
        security.keyFileFailure = SecurityFailure.KeyFileTooLarge()

        expectFailure<SecurityFailure.KeyFileTooLarge> { editor.setKeyFile(DB, KEY) }

        assertEquals(emptyList<CredentialsChange>(), security.credentialChanges)
        assertEquals(emptyList<String>(), journal)
    }

    @Test
    fun `a key file the core refused is wiped and not remembered`() = runTest {
        val refusing = object : SecurityRepository by security {
            override suspend fun changeCredentials(dbKey: String, change: CredentialsChange) {
                throw SecurityFailure.Unexpected()
            }
        }
        val editor = SecurityEditor(refusing, database, biometrics, remembered)

        expectFailure<SecurityFailure.Unexpected> { editor.setKeyFile(DB, KEY) }

        assertTrue(security.keyFilesRead.single().content.all { it == 0.toByte() })
        assertEquals(emptyList<String>(), journal)
    }

    @Test
    fun `a key file not written is remembered all the same, and not saved is what is said`() = runTest {
        database.saveFailure = IOException("provider gone")

        expectFailure<SecurityFailure.NotSaved> { editor.setKeyFile(DB, KEY) }
        assertEquals(listOf("remember $DB $KEY"), journal)
    }

    @Test
    fun `a key file saved but not remembered says so`() = runTest {
        remembered.failure = IOException("store gone")

        expectFailure<SecurityFailure.KeyFileNotRemembered> { editor.setKeyFile(DB, KEY) }
        assertEquals(listOf("save"), database.calls)
    }

    @Test
    fun `the key file goes from a database with a password, and none is remembered`() = runTest {
        security.security = security.security.copy(credentials = CredentialsUsed(password = true, keyFile = true))

        editor.removeKeyFile(DB)

        assertEquals(listOf<CredentialsChange>(CredentialsChange.RemoveKeyFile), security.credentialChanges)
        assertEquals(listOf("save"), database.calls)
        assertEquals(listOf("remember $DB null"), journal)
    }

    @Test
    fun `the only key file cannot go`() = runTest {
        security.security = security.security.copy(credentials = CredentialsUsed(password = false, keyFile = true))

        expectFailure<SecurityFailure.LastFactor> { editor.removeKeyFile(DB) }

        assertEquals(emptyList<CredentialsChange>(), security.credentialChanges)
        assertEquals(emptyList<String>(), journal)
    }

    private companion object {
        const val DB = "content://files/passwords.kdbx"
        const val KEY = "content://keys/home.keyx"
        const val NEW_PASSWORD = "new password 12"
    }
}
