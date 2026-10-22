package ru.kino.dev.ffi

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.kino.dev.core.Cipher
import ru.kino.dev.core.CredentialsChange
import ru.kino.dev.core.CredentialsUsed
import ru.kino.dev.core.DatabaseSecurity
import ru.kino.dev.core.Encryption
import ru.kino.dev.core.KdfVariant
import ru.kino.dev.core.KeyFile

/**
 * The security of the settings both ways, and the four flags the core takes put together from one change
 * (pass-docs, Step 67). What the core does with a combination is its own tests; here - that each change gives the
 * one combination `set_db_settings` documents, whatever the database has now.
 */
class SecurityMappingTest {

    @Test
    fun `the security is read out of the settings`() {
        assertEquals(
            DatabaseSecurity(
                encryption = Encryption(Cipher.ChaCha20, KdfVariant.Argon2d, iterations = 12),
                memoryBytes = 32L * 1024 * 1024,
                parallelism = 4,
                credentials = CredentialsUsed(password = true, keyFile = true),
                keyFileName = "home.keyx",
            ),
            settings(passwordUsed = true, keyFileUsed = true).securityToDomain(),
        )
    }

    @Test
    fun `the encryption changes the cipher, the variant and the passes and nothing else`() {
        val read = settings(passwordUsed = true, keyFileUsed = false)

        val sent = read.withEncryption(Encryption(Cipher.Aes256, KdfVariant.Argon2id, iterations = 3))

        assertEquals(
            read.copy(cipherId = "Aes256", kdf = read.kdf.copy(algorithm = "Argon2id", iterations = 3)),
            sent,
        )
    }

    @Test
    fun `a new password goes with its own change, the key file stays unchanged`() {
        val sent = settings(passwordUsed = true, keyFileUsed = true).withCredentials(SET_PASSWORD)

        assertFlags(sent, password = "new password 12", passwordUsed = true, passwordChanged = true, keyFileUsed = true, keyFileChanged = false)
    }

    @Test
    fun `the first password of a database with only a key file keeps the key file`() {
        val sent = settings(passwordUsed = false, keyFileUsed = true).withCredentials(SET_PASSWORD)

        assertFlags(sent, password = "new password 12", passwordUsed = true, passwordChanged = true, keyFileUsed = true, keyFileChanged = false)
    }

    @Test
    fun `a first key file keeps the password and sends none`() {
        val sent = settings(passwordUsed = true, keyFileUsed = false).withCredentials(SET_KEY_FILE)

        assertFlags(sent, password = null, passwordUsed = true, passwordChanged = false, keyFileUsed = true, keyFileChanged = true)
    }

    @Test
    fun `another key file replaces the one there is`() {
        val sent = settings(passwordUsed = true, keyFileUsed = true).withCredentials(SET_KEY_FILE)

        assertFlags(sent, password = null, passwordUsed = true, passwordChanged = false, keyFileUsed = true, keyFileChanged = true)
    }

    @Test
    fun `removing the key file keeps the password`() {
        val sent = settings(passwordUsed = true, keyFileUsed = true).withCredentials(CredentialsChange.RemoveKeyFile)

        assertFlags(sent, password = null, passwordUsed = true, passwordChanged = false, keyFileUsed = false, keyFileChanged = true)
    }

    // The flags say so, and the core refuses it with InSufficientCredentials before anything changes: nothing here
    // turns it into another combination that would pass
    @Test
    fun `removing the only key file says both are gone`() {
        val sent = settings(passwordUsed = false, keyFileUsed = true).withCredentials(CredentialsChange.RemoveKeyFile)

        assertFlags(sent, password = null, passwordUsed = false, passwordChanged = false, keyFileUsed = false, keyFileChanged = true)
    }

    // Settings are read from the core with no change in them; one kept from an earlier change must not carry over
    @Test
    fun `a change carries no password and no change from what the settings held before`() {
        val held = settings(passwordUsed = true, keyFileUsed = true)
            .copy(password = "old one", passwordChanged = true, keyFileChanged = true)

        val sent = held.withCredentials(CredentialsChange.RemoveKeyFile)

        assertFlags(sent, password = null, passwordUsed = true, passwordChanged = false, keyFileUsed = false, keyFileChanged = true)
    }

    private fun assertFlags(
        sent: DbSettingsDto,
        password: String?,
        passwordUsed: Boolean,
        passwordChanged: Boolean,
        keyFileUsed: Boolean,
        keyFileChanged: Boolean,
    ) {
        assertEquals(
            listOf(password, passwordUsed, passwordChanged, keyFileUsed, keyFileChanged),
            listOf(sent.password, sent.passwordUsed, sent.passwordChanged, sent.keyFileUsed, sent.keyFileChanged),
        )
    }

    private fun settings(passwordUsed: Boolean, keyFileUsed: Boolean) = DbSettingsDto(
        kdf = KdfSettingsDto(algorithm = "Argon2d", memory = 32L * 1024 * 1024, iterations = 12, parallelism = 4, version = 19),
        cipherId = "ChaCha20",
        keyFileName = "home.keyx".takeIf { keyFileUsed },
        passwordUsed = passwordUsed,
        keyFileUsed = keyFileUsed,
        passwordChanged = false,
        keyFileChanged = false,
        databaseFileName = "content://test/security.kdbx",
        kdbxVersion = "4.1",
        compression = "GZip",
        meta = DbMetaDto(
            databaseName = "Passwords",
            databaseDescription = "",
            historyMaxItems = 10,
            historyMaxSize = 6L * 1024 * 1024,
            memoryProtection = MemoryProtectionDto(title = false, userName = false, password = true, url = false, notes = false),
            recycleBinEnabled = true,
        ),
    )

    private companion object {
        val SET_PASSWORD = CredentialsChange.SetPassword("new password 12")
        val SET_KEY_FILE = CredentialsChange.SetKeyFile(KeyFile("new.keyx", byteArrayOf(1, 2, 3)))
    }
}
