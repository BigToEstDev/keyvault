package ru.kino.dev.vault.file

import kotlinx.coroutines.test.runTest
import kotlin.test.assertEquals
import kotlin.test.Test
import ru.kino.dev.core.Compression
import ru.kino.dev.core.expectFailure
import ru.kino.dev.vault.FakeVaultDatabase
import java.io.IOException

class DatabaseFileEditorTest {

    private val files = FakeDatabaseFileRepository()
    private val database = FakeVaultDatabase()
    private val editor = DatabaseFileEditor(files, database)

    @Test
    fun `renaming keeps the name without the spaces around it, then saves`() = runTest {
        val kept = editor.rename(DB, "  Home ")

        assertEquals("Home", kept)
        assertEquals(listOf("Home/GZip"), files.changes)
        assertEquals(listOf("save"), database.calls)
    }

    @Test
    fun `an empty name is refused before anything changes`() = runTest {
        expectFailure<DatabaseFileFailure.EmptyName> { editor.rename(DB, "   ") }

        assertEquals(emptyList<String>(), files.changes)
        assertEquals(emptyList<String>(), database.calls)
    }

    @Test
    fun `the same name is not a change - nothing is written`() = runTest {
        editor.rename(DB, " Passwords ")

        assertEquals(emptyList<String>(), files.changes)
        assertEquals(emptyList<String>(), database.calls)
    }

    @Test
    fun `the compression chosen keeps the name and saves`() = runTest {
        editor.setCompression(DB, Compression.None)

        assertEquals(listOf("Passwords/None"), files.changes)
        assertEquals(listOf("save"), database.calls)
    }

    @Test
    fun `the same compression is not a change - nothing is written`() = runTest {
        editor.setCompression(DB, Compression.GZip)

        assertEquals(emptyList<String>(), database.calls)
    }

    @Test
    fun `a file not written is not saved, and the change stays in memory`() = runTest {
        database.saveFailure = IOException("provider gone")

        expectFailure<DatabaseFileFailure.NotSaved> { editor.setCompression(DB, Compression.None) }

        assertEquals(Compression.None, files.info.compression)
    }

    private companion object {
        const val DB = "content://docs/passwords.kdbx"
    }
}
