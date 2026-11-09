package ru.kino.dev.vault.data

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import ru.kino.dev.core.CoreException
import ru.kino.dev.core.DatabaseFileException
import ru.kino.dev.core.FakeDatabaseFiles
import ru.kino.dev.core.FakeNativeCore
import ru.kino.dev.core.NotAsWrittenException
import ru.kino.dev.core.OpenedDatabase
import ru.kino.dev.core.expectFailure
import ru.kino.dev.vault.data.changes.VaultChangesImpl
import ru.kino.dev.vault.sync.MergeFailure
import ru.kino.dev.vault.sync.SaveState
import ru.kino.dev.vault.sync.SaveStatus

class VaultDatabaseRepositoryImplTest {

    private val journal = mutableListOf<String>()
    private val core = FakeNativeCore(journal)
    private val files = FakeDatabaseFiles(journal)
    private val changes = VaultChangesImpl()
    private val status = SaveStatus()
    private val repository = VaultDatabaseRepositoryImpl(core, files, changes, status)

    @Test
    fun `saving writes the bytes the core gave into the file, then takes them as the file`() = runTest {
        files.contents[DB] = byteArrayOf(1)

        repository.save(DB)

        assertContentEquals(core.databaseBytes, files.contents[DB])
        assertContentEquals(core.databaseBytes, core.checksumBytes[DB])
        assertEquals(
            listOf("read $DB", "verifyFileChecksum $DB", "saveDatabase $DB", "writeReplacing $DB", "setFileChecksum $DB"),
            journal,
        )
        assertEquals(SaveState.Saved, status.current(DB))
    }

    @Test
    fun `a file written elsewhere is not written over - the change waits and the status says so`() = runTest {
        files.contents[DB] = byteArrayOf(9)
        core.changedElsewhere += DB
        val postponed = async(start = CoroutineStart.UNDISPATCHED) { status.postponed.first() }

        repository.save(DB)

        assertContentEquals(byteArrayOf(9), files.contents[DB])
        assertFalse("saveDatabase $DB" in journal)
        assertEquals(SaveState.ChangedElsewhere, status.current(DB))
        assertEquals(DB, postponed.await())
    }

    @Test
    fun `a file that cannot be read for the check is written as always`() = runTest {
        repository.save(DB)

        assertContentEquals(core.databaseBytes, files.contents[DB])
    }

    @Test
    fun `closing forgets what waited`() = runTest {
        status.notSaved(DB)

        repository.close(DB)

        assertEquals(SaveState.Saved, status.current(DB))
    }

    @Test
    fun `a file that cannot be written fails the save, its checksum left as it was`() = runTest {
        files.unreachable += DB

        expectFailure<DatabaseFileException> { repository.save(DB) }

        assertFalse(DB in core.checksumBytes)
        assertEquals(SaveState.NotSaved, status.current(DB))
    }

    @Test
    fun `a file that does not hold the write is damaged, its checksum left as it was`() = runTest {
        files.notAsWritten += DB

        expectFailure<NotAsWrittenException> { repository.save(DB) }

        assertFalse(DB in core.checksumBytes)
        assertEquals(SaveState.Damaged, status.current(DB))
    }

    @Test
    fun `a damaged file is not taken for written elsewhere`() = runTest {
        status.damaged(DB)
        files.contents[DB] = byteArrayOf(9)
        core.changedElsewhere += DB

        assertFalse(repository.hasChangedElsewhere(DB, repository.read(DB)))
    }

    @Test
    fun `the save after a damaged one writes the file again without asking`() = runTest {
        status.damaged(DB)
        files.contents[DB] = byteArrayOf(9)
        core.changedElsewhere += DB

        repository.save(DB)

        assertContentEquals(core.databaseBytes, files.contents[DB])
        assertContentEquals(core.databaseBytes, core.checksumBytes[DB])
        assertEquals(SaveState.Saved, status.current(DB))
    }

    @Test
    fun `a damaged file that cannot be written either stays damaged`() = runTest {
        status.damaged(DB)
        files.unwritable += DB

        expectFailure<DatabaseFileException> { repository.save(DB) }

        assertEquals(SaveState.Damaged, status.current(DB))
    }

    @Test
    fun `locking locks the open database in the core, without saving it`() = runTest {
        core.opened[DB] = OpenedDatabase(DB, "Passwords", null)

        repository.lock(DB)

        assertTrue(DB in core.locked)
        assertEquals(listOf("lockDatabase $DB"), journal)
    }

    @Test
    fun `locking a database the core does not hold is a failure`() = runTest {
        val error = expectFailure<CoreException> { repository.lock(DB) }
        assertEquals("DbKeyNotFound", error.kind)
    }

    @Test
    fun `closing closes the database in the core`() = runTest {
        repository.close(DB)

        assertEquals(listOf("closeDatabase $DB"), journal)
    }

    @Test
    fun `a file that cannot be read now is said by the failure of the merge`() = runTest {
        files.unreachable += DB

        expectFailure<MergeFailure.FileUnreachable> { repository.read(DB) }
    }

    @Test
    fun `a file the checksum still matches has not changed`() = runTest {
        files.contents[DB] = byteArrayOf(1, 2)

        assertFalse(repository.hasChangedElsewhere(DB, repository.read(DB)))
        assertContentEquals(byteArrayOf(1, 2), core.bytesReceived)
    }

    @Test
    fun `a file the checksum no longer matches has changed - an answer, not a failure`() = runTest {
        files.contents[DB] = byteArrayOf(1, 2)
        core.changedElsewhere += DB

        assertTrue(repository.hasChangedElsewhere(DB, repository.read(DB)))
    }

    @Test
    fun `any other refusal of the core while checking is a failure`() = runTest {
        files.contents[DB] = byteArrayOf(1, 2)
        core.failures["verifyFileChecksum"] = CoreException("DbKeyNotFound", "not open")

        val error = expectFailure<CoreException> { repository.hasChangedElsewhere(DB, repository.read(DB)) }
        assertEquals("DbKeyNotFound", error.kind)
    }

    @Test
    fun `merging hands the current file to the core and reports its summary`() = runTest {
        files.contents[DB] = byteArrayOf(7)

        val summary = repository.merge(DB, repository.read(DB))

        assertEquals(core.mergeSummary, summary)
        assertContentEquals(byteArrayOf(7), core.bytesReceived)
    }

    @Test
    fun `a file saved elsewhere with other credentials cannot be merged - said by its own failure`() = runTest {
        files.contents[DB] = byteArrayOf(7)
        core.failures["mergeDatabase"] = CoreException("MergeFailedCredentialsChanged", "other key")

        expectFailure<MergeFailure.CredentialsChanged> { repository.merge(DB, repository.read(DB)) }
    }

    @Test
    fun `another database in the file is not merged`() = runTest {
        files.contents[DB] = byteArrayOf(7)
        core.failures["mergeDatabase"] = CoreException("NotSameDatabase", "another")

        expectFailure<MergeFailure.NotSameDatabase> { repository.merge(DB, repository.read(DB)) }
    }

    private companion object {
        const val DB = "content://docs/passwords.kdbx"
    }
}
