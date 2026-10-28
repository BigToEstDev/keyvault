package ru.kino.dev.vault.trash

import kotlinx.coroutines.test.runTest
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.Test
import ru.kino.dev.core.expectFailure
import ru.kino.dev.vault.FakeVaultDatabase
import ru.kino.dev.vault.entry.EntrySummary
import ru.kino.dev.vault.group.EntryId
import ru.kino.dev.vault.group.GroupId
import java.io.IOException

class TrashEditorTest {

    private val trash = FakeTrashRepository().apply {
        entries += binned("mail")
        entries += binned("bank")
    }
    private val database = FakeVaultDatabase()
    private val editor = TrashEditor(trash, database)

    @Test
    fun `switching the bin off empties it and saves`() = runTest {
        editor.setEnabled(DB, false)

        assertFalse(trash.enabled)
        assertTrue(trash.entries.isEmpty())
        assertEquals(listOf("save"), database.calls)
    }

    @Test
    fun `the same switch again is not a change - nothing is written`() = runTest {
        editor.setEnabled(DB, true)

        assertEquals(listOf("enabled"), trash.calls)
        assertEquals(emptyList<String>(), database.calls)
    }

    @Test
    fun `restored entries go to the group and the file is saved`() = runTest {
        editor.restore(DB, listOf(EntryId("mail")), HOME)

        assertEquals(mapOf(EntryId("mail") to HOME), trash.restored)
        assertEquals(listOf("save"), database.calls)
    }

    @Test
    fun `nothing picked is not a change`() = runTest {
        editor.restore(DB, emptyList(), HOME)
        editor.remove(DB, emptyList())

        assertTrue(trash.calls.isEmpty())
        assertEquals(emptyList<String>(), database.calls)
    }

    @Test
    fun `removed entries leave the bin and the file is saved`() = runTest {
        editor.remove(DB, listOf(EntryId("bank")))

        assertEquals(listOf(EntryId("mail")), trash.entries.map { it.entry.id })
        assertEquals(listOf("save"), database.calls)
    }

    @Test
    fun `emptying takes everything and saves`() = runTest {
        editor.empty(DB)

        assertTrue(trash.entries.isEmpty())
        assertEquals(listOf("save"), database.calls)
    }

    @Test
    fun `a file not written is not saved, and the change stays in memory`() = runTest {
        database.saveFailure = IOException("provider gone")

        expectFailure<TrashFailure.NotSaved> { editor.empty(DB) }

        assertTrue(trash.entries.isEmpty())
    }

    @Test
    fun `a stale list changes nothing and writes nothing`() = runTest {
        expectFailure<TrashFailure.Stale> { editor.remove(DB, listOf(EntryId("gone"))) }

        assertEquals(2, trash.entries.size)
        assertEquals(emptyList<String>(), database.calls)
    }

    private fun binned(id: String) = TrashEntry(
        entry = EntrySummary(EntryId(id), GroupId("bin"), id, "", createdAt = 0),
        previousGroupId = null,
        deletedAt = 0,
    )

    private companion object {
        const val DB = "content://docs/passwords.kdbx"
        val HOME = GroupId("home")
    }
}
