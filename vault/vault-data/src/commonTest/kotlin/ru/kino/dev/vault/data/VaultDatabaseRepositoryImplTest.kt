package ru.kino.dev.vault.data

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import ru.kino.dev.core.CoreException
import ru.kino.dev.core.DatabaseFileException
import ru.kino.dev.core.FakeDatabaseFiles
import ru.kino.dev.core.FakeNativeCore
import ru.kino.dev.core.OpenedDatabase
import ru.kino.dev.core.expectFailure

class VaultDatabaseRepositoryImplTest {

    private val journal = mutableListOf<String>()
    private val core = FakeNativeCore(journal)
    private val files = FakeDatabaseFiles(journal)
    private val repository = VaultDatabaseRepositoryImpl(core, files)

    @Test
    fun `saving writes the bytes the core gave into the file`() = runTest {
        repository.save(DB)

        assertContentEquals(core.databaseBytes, files.contents[DB])
        assertEquals(listOf("saveDatabase $DB", "writeReplacing $DB"), journal)
    }

    @Test
    fun `a file that cannot be written fails the save`() = runTest {
        files.unreachable += DB

        expectFailure<DatabaseFileException> { repository.save(DB) }
    }

    @Test
    fun `locking locks the open database in the core, without saving it`() = runTest {
        core.opened[DB] = OpenedDatabase(DB, "Passwords", "passwords.kdbx", null)

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
    fun `a file the checksum still matches has not changed`() = runTest {
        files.contents[DB] = byteArrayOf(1, 2)

        assertFalse(repository.hasChangedElsewhere(DB))
        assertContentEquals(byteArrayOf(1, 2), core.bytesReceived)
    }

    @Test
    fun `a file the checksum no longer matches has changed - an answer, not a failure`() = runTest {
        files.contents[DB] = byteArrayOf(1, 2)
        core.changedElsewhere += DB

        assertTrue(repository.hasChangedElsewhere(DB))
    }

    @Test
    fun `any other refusal of the core while checking is a failure`() = runTest {
        files.contents[DB] = byteArrayOf(1, 2)
        core.failures["verifyFileChecksum"] = CoreException("DbKeyNotFound", "not open")

        val error = expectFailure<CoreException> { repository.hasChangedElsewhere(DB) }
        assertEquals("DbKeyNotFound", error.kind)
    }

    @Test
    fun `merging hands the current file to the core and reports its summary`() = runTest {
        files.contents[DB] = byteArrayOf(7)

        val summary = repository.merge(DB)

        assertEquals(core.mergeSummary, summary)
        assertContentEquals(byteArrayOf(7), core.bytesReceived)
    }

    private companion object {
        const val DB = "content://docs/passwords.kdbx"
    }
}
