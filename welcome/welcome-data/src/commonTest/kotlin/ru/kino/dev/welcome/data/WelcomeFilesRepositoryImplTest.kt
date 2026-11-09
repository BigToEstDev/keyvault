package ru.kino.dev.welcome.data

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import ru.kino.dev.core.CredentialLimits
import ru.kino.dev.core.FakeDatabaseFiles
import ru.kino.dev.core.FakeNativeCore
import ru.kino.dev.core.expectFailure
import ru.kino.dev.welcome.PickedFile
import ru.kino.dev.welcome.WelcomeFailure

class WelcomeFilesRepositoryImplTest {

    private val journal = mutableListOf<String>()
    private val core = FakeNativeCore(journal)
    private val files = FakeDatabaseFiles(journal)
    private val repository = WelcomeFilesRepositoryImpl(core, files)

    @Test
    fun `an empty file left by save as is deleted`() = runTest {
        files.contents[KEY] = byteArrayOf()

        repository.discardEmpty(KEY)

        assertFalse(KEY in files.contents)
    }

    @Test
    fun `a picked file with content is not deleted`() = runTest {
        files.contents[KEY] = byteArrayOf(1)

        repository.discardEmpty(KEY)

        assertContentEquals(byteArrayOf(1), files.contents[KEY])
        assertFalse("delete $KEY" in journal)
    }

    @Test
    fun `an empty file the provider will not delete, or one out of reach, is left without a failure`() = runTest {
        files.contents[KEY] = byteArrayOf()
        files.undeletable += KEY
        files.unreachable += "content://keys/gone.keyx"

        repository.discardEmpty(KEY)
        repository.discardEmpty("content://keys/gone.keyx")

        assertTrue(KEY in files.contents)
    }

    @Test
    fun `a picked file shows the name the provider tells`() = runTest {
        files.names[KEY] = "my.keyx"

        assertEquals("my.keyx", repository.displayName(KEY))
        assertNull(repository.displayName("content://keys/nameless"))
    }

    @Test
    fun `a database deleted outside the app, or out of reach, is not reachable - asked without reading it`() =
        runTest {
            files.contents[KEY] = byteArrayOf(1)

            assertTrue(repository.isReachable(KEY))
            assertFalse(repository.isReachable("content://docs/deleted.kdbx"))
            files.unreachable += KEY
            assertFalse(repository.isReachable(KEY))
            assertFalse(journal.any { it.startsWith("read") }, journal.toString())
        }

    @Test
    fun `an existing file picked as a key file is checked by its size, not read`() = runTest {
        files.contents[KEY] = byteArrayOf(1)
        files.reportedSizes[KEY] = CredentialLimits.KEY_FILE_MAX_SIZE

        repository.checkKeyFile(KEY)

        assertFalse(journal.any { it.startsWith("read") }, journal.toString())
    }

    @Test
    fun `a picked key file over the limit is refused right away`() = runTest {
        files.contents[KEY] = byteArrayOf(1)
        files.reportedSizes[KEY] = CredentialLimits.KEY_FILE_MAX_SIZE + 1

        expectFailure<WelcomeFailure.KeyFileTooLarge> { repository.checkKeyFile(KEY) }
    }

    @Test
    fun `a picked key file that is not there is no access to the key file`() = runTest {
        val failure = expectFailure<WelcomeFailure.FileUnreachable> { repository.checkKeyFile(KEY) }

        assertEquals(PickedFile.KeyFile, failure.file)
    }

    @Test
    fun `a new file from save as would overwrite nothing`() = runTest {
        files.contents[KEY] = ByteArray(0)

        assertFalse(repository.wouldOverwrite(KEY, PickedFile.KeyFile))
    }

    @Test
    fun `an existing file picked in save as would be overwritten`() = runTest {
        files.contents[KEY] = byteArrayOf(1)

        assertTrue(repository.wouldOverwrite(KEY, PickedFile.KeyFile))
    }

    @Test
    fun `a file out of reach when asked about overwriting names the file asked about`() = runTest {
        files.unreachable += DB

        val failure = expectFailure<WelcomeFailure.FileUnreachable> {
            repository.wouldOverwrite(DB, PickedFile.Database)
        }
        assertEquals(PickedFile.Database, failure.file)
    }

    @Test
    fun `a new key file goes from the core straight into the picked file`() = runTest {
        files.contents[KEY] = ByteArray(0)

        repository.writeNewKeyFile(KEY)

        assertContentEquals(core.generatedKeyFile, files.contents[KEY])
        assertEquals(listOf("generateKeyFile", "writeReplacing $KEY"), journal)
    }

    @Test
    fun `a key file that cannot be written is no access to the key file`() = runTest {
        files.unwritable += KEY

        val failure = expectFailure<WelcomeFailure.FileUnreachable> { repository.writeNewKeyFile(KEY) }
        assertEquals(PickedFile.KeyFile, failure.file)
    }

    private companion object {
        const val DB = "content://docs/passwords.kdbx"
        const val KEY = "content://keys/my.keyx"
    }
}
