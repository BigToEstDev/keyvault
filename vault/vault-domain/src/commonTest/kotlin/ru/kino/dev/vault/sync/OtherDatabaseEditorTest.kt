package ru.kino.dev.vault.sync

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import ru.kino.dev.core.DatabaseFileException
import ru.kino.dev.core.expectFailure
import ru.kino.dev.vault.FakeVaultDatabase
import ru.kino.dev.vault.lock.DatabaseGuard

class OtherDatabaseEditorTest {

    private val other = FakeOtherDatabaseRepository()
    private val database = FakeVaultDatabase()
    private val status = SaveStatus()
    private val editor = OtherDatabaseEditor(other, database, status, DatabaseGuard())
    private val credentials = OtherDatabaseCredentials("pw", null)

    @Test
    fun `a copy is merged, then written`() = runTest {
        val summary = editor.bringIn(DB, OtherDatabaseKind.Copy, FILE, credentials)

        assertSame(other.summary, summary)
        assertEquals(listOf("mergeCopy $FILE"), other.calls)
        assertEquals(listOf("save"), database.calls)
    }

    @Test
    fun `another database is imported, then written`() = runTest {
        editor.bringIn(DB, OtherDatabaseKind.Import, FILE, credentials)

        assertEquals(listOf("importDatabase $FILE"), other.calls)
        assertEquals(listOf("save"), database.calls)
    }

    @Test
    fun `a merge refused writes nothing`() = runTest {
        other.failure = MergeFailure.NotSameDatabase()

        expectFailure<MergeFailure.NotSameDatabase> { editor.bringIn(DB, OtherDatabaseKind.Copy, FILE, credentials) }

        assertTrue(database.calls.isEmpty())
    }

    @Test
    fun `a merge not written is said to be not written`() = runTest {
        database.saveFailure = DatabaseFileException("disk full")

        expectFailure<MergeFailure.NotSaved> { editor.bringIn(DB, OtherDatabaseKind.Copy, FILE, credentials) }
    }

    @Test
    fun `the file of the open database is refused before anything changes`() = runTest {
        expectFailure<MergeFailure.OwnFile> { editor.bringIn(DB, OtherDatabaseKind.Copy, DB, credentials) }

        assertTrue(other.calls.isEmpty())
        assertTrue(database.calls.isEmpty())
    }

    @Test
    fun `both ways the copy is written after this database`() = runTest {
        val summary = editor.bringIn(DB, OtherDatabaseKind.CopyBothWays, FILE, credentials)

        assertSame(other.summary, summary)
        assertEquals(listOf("mergeCopyBothWays $FILE", "writeCopy $FILE"), other.calls)
        assertEquals(listOf("save"), database.calls)
        assertContentEquals(other.copyBytes, other.copyWritten)
    }

    @Test
    fun `both ways this database not written leaves the copy as it was`() = runTest {
        database.saveFailure = DatabaseFileException("disk full")

        expectFailure<MergeFailure.NotSaved> { editor.bringIn(DB, OtherDatabaseKind.CopyBothWays, FILE, credentials) }

        assertEquals(listOf("mergeCopyBothWays $FILE"), other.calls)
    }

    @Test
    fun `both ways this file changed elsewhere leaves the copy as it was`() = runTest {
        // The save found the file written elsewhere and did not write it
        status.changedElsewhere(DB)

        expectFailure<MergeFailure.CopyNotWrittenChangedElsewhere> {
            editor.bringIn(DB, OtherDatabaseKind.CopyBothWays, FILE, credentials)
        }

        assertEquals(listOf("mergeCopyBothWays $FILE"), other.calls)
    }

    @Test
    fun `both ways a copy not written is said so`() = runTest {
        other.writeFailure = MergeFailure.CopyNotWritten()

        expectFailure<MergeFailure.CopyNotWritten> {
            editor.bringIn(DB, OtherDatabaseKind.CopyBothWays, FILE, credentials)
        }

        assertEquals(listOf("save"), database.calls)
    }

    private companion object {
        const val DB = "content://docs/passwords.kdbx"
        const val FILE = "content://downloads/passwords-phone.kdbx"
    }
}
