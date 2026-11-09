package ru.kino.dev.vault.sync

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import ru.kino.dev.core.DatabaseFileException
import ru.kino.dev.core.expectFailure
import ru.kino.dev.vault.FakeVaultDatabase
import ru.kino.dev.vault.lock.DatabaseGuard

class DatabaseCopyEditorTest {

    private val copies = FakeDatabaseCopyRepository()
    private val database = FakeVaultDatabase()
    private val status = SaveStatus()
    private val editor = DatabaseCopyEditor(copies, database, status, DatabaseGuard())

    @Test
    fun `a saved database is copied as its file is, without a save`() = runTest {
        editor.saveCopy(DB, FILE)

        assertEquals(listOf("read $DB", "write $FILE"), copies.calls)
        assertContentEquals(copies.fileBytes, copies.written)
        assertTrue(database.calls.isEmpty())
    }

    @Test
    fun `a change waiting for its save is saved first`() = runTest {
        status.notSaved(DB)

        editor.saveCopy(DB, FILE)

        assertEquals(listOf("save"), database.calls)
        assertEquals(listOf("read $DB", "write $FILE"), copies.calls)
    }

    @Test
    fun `a file damaged by a write is saved again first`() = runTest {
        status.damaged(DB)

        editor.saveCopy(DB, FILE)

        assertEquals(listOf("save"), database.calls)
        assertEquals(listOf("read $DB", "write $FILE"), copies.calls)
    }

    @Test
    fun `a save that fails copies nothing`() = runTest {
        status.notSaved(DB)
        database.saveFailure = DatabaseFileException("disk full")

        expectFailure<DatabaseCopyFailure.NotSaved> { editor.saveCopy(DB, FILE) }

        assertTrue(copies.calls.isEmpty())
    }

    @Test
    fun `a save that finds the file written elsewhere copies nothing`() = runTest {
        status.notSaved(DB)
        database.onSave = { status.changedElsewhere(DB) }

        expectFailure<DatabaseCopyFailure.ChangedElsewhere> { editor.saveCopy(DB, FILE) }

        assertTrue(copies.calls.isEmpty())
    }

    @Test
    fun `a file written elsewhere is not copied`() = runTest {
        status.changedElsewhere(DB)

        expectFailure<DatabaseCopyFailure.ChangedElsewhere> { editor.saveCopy(DB, FILE) }

        assertTrue(copies.calls.isEmpty())
        assertTrue(database.calls.isEmpty())
    }

    @Test
    fun `the file of the open database is refused`() = runTest {
        expectFailure<DatabaseCopyFailure.OwnFile> { editor.saveCopy(DB, DB) }

        assertTrue(copies.calls.isEmpty())
    }

    @Test
    fun `a copy not written is said so`() = runTest {
        copies.writeFailure = DatabaseCopyFailure.NotWritten()

        expectFailure<DatabaseCopyFailure.NotWritten> { editor.saveCopy(DB, FILE) }
    }

    @Test
    fun `a file that cannot be read is said so`() = runTest {
        copies.readFailure = DatabaseCopyFailure.Unreadable()

        expectFailure<DatabaseCopyFailure.Unreadable> { editor.saveCopy(DB, FILE) }

        assertEquals(listOf("read $DB"), copies.calls)
    }

    @Test
    fun `a file is shared under its own name`() = runTest {
        val uri = editor.share(DB)

        assertEquals(copies.shareUri, uri)
        assertEquals(listOf("read $DB", "prepareShare base.kdbx"), copies.calls)
        assertContentEquals(copies.fileBytes, copies.written)
    }

    @Test
    fun `a file the provider names nothing is shared under the fallback name`() = runTest {
        copies.fileName = null

        editor.share(DB)

        assertEquals("prepareShare ${DatabaseCopyEditor.FALLBACK_NAME}", copies.calls.last())
    }

    @Test
    fun `a file written elsewhere is not shared`() = runTest {
        status.changedElsewhere(DB)

        expectFailure<DatabaseCopyFailure.ChangedElsewhere> { editor.share(DB) }

        assertTrue(copies.calls.isEmpty())
    }

    private companion object {
        const val DB = "content://db"
        const val FILE = "content://copy"
    }
}
