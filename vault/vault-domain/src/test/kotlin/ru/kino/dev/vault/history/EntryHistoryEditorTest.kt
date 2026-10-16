package ru.kino.dev.vault.history

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.kino.dev.core.CoreException
import ru.kino.dev.core.expectFailure
import ru.kino.dev.core.preset.StandardPreset.TITLE
import ru.kino.dev.vault.FakeVaultDatabase
import ru.kino.dev.vault.entry.EntryFailure
import ru.kino.dev.vault.entry.EntryField
import ru.kino.dev.vault.entry.FakeEntryRepository
import ru.kino.dev.vault.group.GroupId

class EntryHistoryEditorTest {

    private val entries = FakeEntryRepository()
    private val database = FakeVaultDatabase()
    private val editor = EntryHistoryEditor(FakeEntryHistoryRepository(entries), database)
    private val mail = entries.add("mail", GroupId("work"), listOf(title("Mail")))

    @Test
    fun `a version restored is the entry again, the state before goes into the history, the file is written`() =
        runTest {
            entries.update(DB, mail, listOf(title("Post")))

            editor.restore(DB, mail, 0)

            val entry = entries.stored.getValue(mail)
            assertEquals(listOf(title("Mail")), entry.fields)
            assertEquals(listOf(listOf(title("Mail")), listOf(title("Post"))), entry.history.map { it.fields })
            assertEquals(listOf("save"), database.calls)
        }

    @Test
    fun `a version deleted leaves the history, the file is written`() = runTest {
        entries.update(DB, mail, listOf(title("Post")))
        entries.update(DB, mail, listOf(title("Box")))

        editor.delete(DB, mail, 0)

        assertEquals(listOf(listOf(title("Post"))), entries.stored.getValue(mail).history.map { it.fields })
        assertEquals(listOf("save"), database.calls)
    }

    @Test
    fun `the whole history deleted, the entry stays as it is, the file is written`() = runTest {
        entries.update(DB, mail, listOf(title("Post")))
        entries.update(DB, mail, listOf(title("Box")))

        editor.deleteAll(DB, mail)

        val entry = entries.stored.getValue(mail)
        assertTrue(entry.history.isEmpty())
        assertEquals(listOf(title("Box")), entry.fields)
        assertEquals(listOf("save"), database.calls)
    }

    @Test
    fun `a change not written is NotSaved, made in memory all the same`() = runTest {
        entries.update(DB, mail, listOf(title("Post")))
        database.saveFailure = CoreException("Io", "disk full")

        expectFailure<EntryFailure.NotSaved> { editor.delete(DB, mail, 0) }

        assertTrue(entries.stored.getValue(mail).history.isEmpty())
    }

    @Test
    fun `a version that is not there is refused, nothing is written`() = runTest {
        expectFailure<EntryFailure.EntryNotFound> { editor.restore(DB, mail, 3) }

        assertTrue(database.calls.isEmpty())
    }

    private fun title(value: String) = EntryField(TITLE, value, protected = false)

    private companion object {
        const val DB = "content://db"
    }
}
