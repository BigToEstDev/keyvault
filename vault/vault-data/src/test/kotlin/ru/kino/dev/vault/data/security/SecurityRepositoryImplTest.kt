package ru.kino.dev.vault.data.security

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import ru.kino.dev.core.CoreException
import ru.kino.dev.core.CredentialLimits
import ru.kino.dev.core.CredentialsChange
import ru.kino.dev.core.FakeDatabaseFiles
import ru.kino.dev.core.FakeNativeCore
import ru.kino.dev.core.OpenedDatabase
import ru.kino.dev.core.expectFailure
import ru.kino.dev.vault.security.SecurityFailure

class SecurityRepositoryImplTest {

    private val core = FakeNativeCore().apply { opened[DB] = OpenedDatabase(DB, "Passwords", "passwords.kdbx", null) }
    private val documents = FakeDatabaseFiles()
    private val repository = SecurityRepositoryImpl(core, documents)

    @Test
    fun `a key file is read with its name and its access kept for reading`() = runTest {
        documents.contents[KEY] = byteArrayOf(7, 8, 9)
        documents.names[KEY] = "home.keyx"

        val keyFile = repository.readKeyFile(KEY)

        assertEquals("home.keyx", keyFile.name)
        assertArrayEquals(byteArrayOf(7, 8, 9), keyFile.content)
        assertEquals(mapOf(KEY to false), documents.kept)
    }

    @Test
    fun `a key file over the limit is refused before it is read`() = runTest {
        documents.contents[KEY] = byteArrayOf(1)
        documents.reportedSizes[KEY] = CredentialLimits.KEY_FILE_MAX_SIZE + 1

        expectFailure<SecurityFailure.KeyFileTooLarge> { repository.readKeyFile(KEY) }
        assertEquals(emptyMap<String, Boolean>(), documents.kept)
    }

    @Test
    fun `a key file that cannot be read says so`() = runTest {
        documents.unreachable += KEY

        expectFailure<SecurityFailure.KeyFileUnreadable> { repository.readKeyFile(KEY) }
    }

    @Test
    fun `a key file whose access cannot be kept is refused and wiped`() = runTest {
        documents.contents[KEY] = byteArrayOf(7, 8, 9)
        documents.notKeepable += KEY

        expectFailure<SecurityFailure.KeyFileUnreadable> { repository.readKeyFile(KEY) }
    }

    @Test
    fun `a refusal of the core is unexpected`() = runTest {
        core.failures["changeDatabaseCredentials"] = CoreException("InSufficientCredentials", "no password left")

        expectFailure<SecurityFailure.Unexpected> {
            repository.changeCredentials(DB, CredentialsChange.RemoveKeyFile)
        }
    }

    private companion object {
        const val DB = "content://files/passwords.kdbx"
        const val KEY = "content://keys/home.keyx"
    }
}
