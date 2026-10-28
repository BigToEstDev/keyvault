package ru.kino.dev.vault.data.trash

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.kino.dev.core.CoreException
import ru.kino.dev.core.EntryFieldData
import ru.kino.dev.core.FakeNativeCore
import ru.kino.dev.core.GroupNode
import ru.kino.dev.core.GroupTree
import ru.kino.dev.core.expectFailure
import ru.kino.dev.vault.group.EntryId
import ru.kino.dev.vault.group.GroupId
import ru.kino.dev.vault.trash.TrashFailure

class TrashRepositoryImplTest {

    private val core = FakeNativeCore()
    private val repository = TrashRepositoryImpl(core)

    init {
        core.groupTrees[DB] = GroupTree(
            rootId = ROOT,
            recycleBinId = null,
            groups = mapOf(
                ROOT to GroupNode(ROOT, null, "Passwords", listOf(WORK, HOME), emptyList()),
                WORK to GroupNode(WORK, ROOT, "Work", emptyList(), emptyList()),
                HOME to GroupNode(HOME, ROOT, "Home", emptyList(), emptyList()),
            ),
        )
        core.addEntry(DB, WORK, "mail", listOf(data("Title", "Mail"), data("UserName", "anton")))
        core.addEntry(DB, HOME, "bank", listOf(data("Title", "Bank")))
        core.addEntry(DB, WORK, "kept", listOf(data("Title", "Kept")))
    }

    @Test
    fun `the bin lists deleted entries with the group they came from and when`() = runTest {
        core.deleteEntry(DB, "mail")

        val mail = repository.entries(DB).single()

        assertEquals(EntryId("mail"), mail.entry.id)
        assertEquals("anton", mail.entry.userName)
        assertEquals(GroupId(WORK), mail.previousGroupId)
        assertTrue(mail.deletedAt > 0)
    }

    @Test
    fun `an entry of a deleted group comes with no group to go back to`() = runTest {
        core.deleteGroup(DB, HOME)

        val bank = repository.entries(DB).single()

        assertEquals(EntryId("bank"), bank.entry.id)
        assertNull(bank.previousGroupId)
    }

    @Test
    fun `a search looks into the bin only and comes back with its term`() = runTest {
        core.deleteEntry(DB, "mail")

        val found = repository.search(DB, "ANT")

        assertEquals("ANT", found.term)
        assertEquals(listOf(EntryId("mail")), found.entries.map { it.entry.id })
        assertTrue(repository.search(DB, "kept").entries.isEmpty())
    }

    @Test
    fun `restored entries go to the group picked`() = runTest {
        core.deleteEntry(DB, "mail")
        core.deleteEntry(DB, "bank")

        repository.restore(DB, listOf(EntryId("mail"), EntryId("bank")), GroupId(HOME))

        assertTrue(repository.entries(DB).isEmpty())
        assertEquals(HOME, core.storedEntries.getValue(DB).getValue("mail").groupId)
    }

    @Test
    fun `removed entries are gone for good, the rest stays in the bin`() = runTest {
        core.deleteEntry(DB, "mail")
        core.deleteEntry(DB, "bank")

        repository.remove(DB, listOf(EntryId("mail")))

        assertEquals(listOf(EntryId("bank")), repository.entries(DB).map { it.entry.id })
        assertFalse("mail" in core.storedEntries.getValue(DB))
    }

    @Test
    fun `emptying takes everything in the bin`() = runTest {
        core.deleteEntry(DB, "mail")

        repository.empty(DB)

        assertTrue(repository.entries(DB).isEmpty())
        assertEquals(setOf("bank", "kept"), core.storedEntries.getValue(DB).keys)
    }

    @Test
    fun `the switch is the core's, and off it removes the bin`() = runTest {
        core.deleteEntry(DB, "mail")

        repository.setEnabled(DB, false)

        assertFalse(repository.enabled(DB))
        assertNull(core.groupTrees.getValue(DB).recycleBinId)
        assertTrue(repository.entries(DB).isEmpty())
    }

    @Test
    fun `an entry no longer in the bin is a stale list, not a bug`() = runTest {
        expectFailure<TrashFailure.Stale> { repository.restore(DB, listOf(EntryId("kept")), GroupId(HOME)) }
        expectFailure<TrashFailure.Stale> { repository.remove(DB, listOf(EntryId("gone"))) }
    }

    @Test
    fun `anything else the core refuses is unexpected`() = runTest {
        core.failures["trashEntries"] = CoreException("DbKeyNotFound", "closed")

        expectFailure<TrashFailure.Unexpected> { repository.entries(DB) }
    }

    private fun data(key: String, value: String) = EntryFieldData(key = key, value = value, protected = false)

    private companion object {
        const val DB = "content://docs/passwords.kdbx"
        const val ROOT = "root"
        const val WORK = "work"
        const val HOME = "home"
    }
}
