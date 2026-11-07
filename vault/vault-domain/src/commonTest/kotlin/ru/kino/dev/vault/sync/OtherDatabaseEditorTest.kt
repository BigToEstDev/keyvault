package ru.kino.dev.vault.sync

import kotlin.test.Test
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
    private val editor = OtherDatabaseEditor(other, database, DatabaseGuard())
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

    private companion object {
        const val DB = "content://docs/passwords.kdbx"
        const val FILE = "content://downloads/passwords-phone.kdbx"
    }
}
