package ru.kino.dev.vault.data.sync

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import ru.kino.dev.core.CoreException
import ru.kino.dev.core.CredentialLimits
import ru.kino.dev.core.FakeDatabaseFiles
import ru.kino.dev.core.FakeNativeCore
import ru.kino.dev.core.OpenedDatabase
import ru.kino.dev.core.expectFailure
import ru.kino.dev.vault.data.changes.VaultChangesImpl
import ru.kino.dev.vault.sync.MergeFailure
import ru.kino.dev.vault.sync.OtherDatabaseCredentials

class OtherDatabaseRepositoryImplTest {

    private val journal = mutableListOf<String>()
    private val core = FakeNativeCore(journal).apply {
        opened[DB] = OpenedDatabase(DB, "Passwords", "passwords.kdbx", null)
    }
    private val files = FakeDatabaseFiles(journal).apply {
        contents[COPY] = byteArrayOf(4, 2)
        contents[KEY] = byteArrayOf(9, 9, 9)
        names[KEY] = "key.keyx"
    }
    private val changes = VaultChangesImpl()
    private val repository = OtherDatabaseRepositoryImpl(core, files, changes)

    @Test
    fun `a copy is merged from the bytes of the file picked, with its own password`() = runTest {
        val summary = repository.mergeCopy(DB, COPY, OtherDatabaseCredentials("copy password", null))

        assertEquals(core.mergeSummary, summary)
        assertContentEquals(byteArrayOf(4, 2), core.bytesReceived)
        assertEquals("copy password", core.passwordReceived)
        assertTrue("mergeCopy $DB" in journal)
    }

    @Test
    fun `an import goes to its own command`() = runTest {
        repository.importDatabase(DB, COPY, OtherDatabaseCredentials("other password", null))

        assertTrue("importDatabase $DB" in journal)
        assertTrue("mergeCopy $DB" !in journal)
    }

    @Test
    fun `the key file is read once, not kept, and wiped after`() = runTest {
        repository.mergeCopy(DB, COPY, OtherDatabaseCredentials(null, KEY))

        val keyFile = core.keyFilesReceived.single()
        assertContentEquals(byteArrayOf(0, 0, 0), keyFile.content)
        assertTrue(files.kept.isEmpty())
    }

    @Test
    fun `the key file is wiped when the merge fails too`() = runTest {
        core.failures["mergeCopy"] = CoreException("InvalidCredentials", "no")

        expectFailure<MergeFailure.WrongCredentials> {
            repository.mergeCopy(DB, COPY, OtherDatabaseCredentials("pw", KEY))
        }

        assertContentEquals(byteArrayOf(0, 0, 0), core.keyFilesReceived.single().content)
    }

    @Test
    fun `a merge tells the screens the database changed`() = runTest {
        val before = changes.current(DB)

        repository.mergeCopy(DB, COPY, OtherDatabaseCredentials("pw", null))

        assertTrue(changes.current(DB) > before)
    }

    @Test
    fun `the core refusing the operation is said by its own failure`() = runTest {
        core.failures["mergeCopy"] = CoreException("NotSameDatabase", "another")
        expectFailure<MergeFailure.NotSameDatabase> {
            repository.mergeCopy(DB, COPY, OtherDatabaseCredentials("pw", null))
        }
    }

    @Test
    fun `an import of a copy is said by its own failure`() = runTest {
        core.failures["importDatabase"] = CoreException("SameDatabase", "a copy")
        expectFailure<MergeFailure.SameDatabase> {
            repository.importDatabase(DB, COPY, OtherDatabaseCredentials("pw", null))
        }
    }

    @Test
    fun `a damaged file is said to be damaged`() = runTest {
        core.failures["mergeCopy"] = CoreException("HeaderCorrupted", "cut short")
        expectFailure<MergeFailure.CorruptedFile> {
            repository.mergeCopy(DB, COPY, OtherDatabaseCredentials("pw", null))
        }
    }

    @Test
    fun `a file that cannot be read is unreachable, the core not asked`() = runTest {
        files.unreachable += COPY

        expectFailure<MergeFailure.FileUnreachable> {
            repository.mergeCopy(DB, COPY, OtherDatabaseCredentials("pw", null))
        }
        assertTrue("mergeCopy $DB" !in journal)
    }

    @Test
    fun `a key file that cannot be read is its own failure`() = runTest {
        files.unreachable += KEY

        expectFailure<MergeFailure.KeyFileUnreadable> {
            repository.mergeCopy(DB, COPY, OtherDatabaseCredentials(null, KEY))
        }
    }

    @Test
    fun `a key file over the limit is refused before it is read whole`() = runTest {
        files.reportedSizes[KEY] = CredentialLimits.KEY_FILE_MAX_SIZE + 1

        expectFailure<MergeFailure.KeyFileTooLarge> {
            repository.mergeCopy(DB, COPY, OtherDatabaseCredentials(null, KEY))
        }
    }

    @Test
    fun `a locked database takes no merge`() = runTest {
        core.locked += DB

        expectFailure<MergeFailure.Locked> {
            repository.mergeCopy(DB, COPY, OtherDatabaseCredentials("pw", null))
        }
    }

    @Test
    fun `both ways the copy is merged with its own password and its bytes come back`() = runTest {
        core.mergedCopyBytes = byteArrayOf(5, 5)

        val merged = repository.mergeCopyBothWays(DB, COPY, OtherDatabaseCredentials("copy password", null))

        assertEquals(core.mergeSummary, merged.summary)
        assertContentEquals(byteArrayOf(5, 5), merged.copy)
        assertContentEquals(byteArrayOf(4, 2), core.bytesReceived)
        assertEquals("copy password", core.passwordReceived)
        assertTrue("mergeCopyBothWays $DB" in journal)
    }

    @Test
    fun `both ways a refusal of the core is a merge failure`() = runTest {
        core.failures["mergeCopyBothWays"] = CoreException("NotSameDatabase", "another")

        expectFailure<MergeFailure.NotSameDatabase> {
            repository.mergeCopyBothWays(DB, COPY, OtherDatabaseCredentials("pw", null))
        }
    }

    @Test
    fun `the copy is written over the file picked, whole`() = runTest {
        repository.writeCopy(COPY, byteArrayOf(1, 2, 3))

        assertContentEquals(byteArrayOf(1, 2, 3), files.contents[COPY])
    }

    @Test
    fun `a copy that cannot be written is not written`() = runTest {
        files.unwritable += COPY

        expectFailure<MergeFailure.CopyNotWritten> { repository.writeCopy(COPY, byteArrayOf(1)) }
        assertContentEquals(byteArrayOf(4, 2), files.contents[COPY])
    }

    private companion object {
        const val DB = "content://docs/passwords.kdbx"
        const val COPY = "content://downloads/passwords-phone.kdbx"
        const val KEY = "content://downloads/key.keyx"
    }
}
