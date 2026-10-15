package ru.kino.dev.vault.data.entry

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import ru.kino.dev.core.CoreException
import ru.kino.dev.core.EntryFieldData
import ru.kino.dev.core.FakeNativeCore
import ru.kino.dev.core.GroupNode
import ru.kino.dev.core.GroupTree
import ru.kino.dev.core.expectFailure
import ru.kino.dev.vault.entry.EntryFailure
import ru.kino.dev.vault.entry.EntryField
import ru.kino.dev.vault.entry.EntryRecord
import ru.kino.dev.vault.group.EntryId
import ru.kino.dev.vault.group.GroupId

class EntryRepositoryImplTest {

    private val core = FakeNativeCore()
    private val repository = EntryRepositoryImpl(core)

    init {
        core.groupTrees[DB] = GroupTree(
            rootId = ROOT,
            recycleBinId = null,
            groups = mapOf(
                ROOT to GroupNode(ROOT, null, "Passwords", listOf(WORK), emptyList()),
                WORK to GroupNode(WORK, ROOT, "Work", emptyList(), emptyList()),
            ),
        )
        core.addEntry(DB, WORK, "mail", listOf(data("Title", "Mail"), data("UserName", "anton")))
        core.addEntry(DB, WORK, "bank", listOf(data("Title", "Bank"), data("PIN", "1", protected = true)))
    }

    @Test
    fun `the entries of a group come by title, with their user name`() = runTest {
        val entries = repository.entries(DB, GroupId(WORK))

        assertEquals(listOf("Bank", "Mail"), entries.map { it.title })
        assertEquals(listOf("", "anton"), entries.map { it.userName })
        assertEquals(EntryId("bank"), entries.first().id)
    }

    @Test
    fun `an entry carries its group and when it was made`() = runTest {
        val bank = repository.entries(DB, GroupId(WORK)).first()

        assertEquals(GroupId(WORK), bank.groupId)
        assertEquals(1_700_000_002L, bank.createdAt)
    }

    @Test
    fun `a search finds by the title or the user name and comes back with its term`() = runTest {
        val found = repository.search(DB, "ANT")

        assertEquals("ANT", found.term)
        assertEquals(listOf(EntryId("mail")), found.entries.map { it.id })
        assertEquals(GroupId(WORK), found.entries.single().groupId)
    }

    @Test
    fun `a search does not look into a protected field`() = runTest {
        assertEquals(emptyList<Any>(), repository.search(DB, "1").entries)
    }

    @Test
    fun `a group that is not there has no entries`() = runTest {
        assertEquals(emptyList<Any>(), repository.entries(DB, GroupId("missing")))
    }

    @Test
    fun `an entry comes with its group and its fields as the core holds them`() = runTest {
        assertEquals(
            EntryRecord(GroupId(WORK), listOf(EntryField("Title", "Bank", false), EntryField("PIN", "1", true))),
            repository.read(DB, EntryId("bank")),
        )
    }

    @Test
    fun `a created entry is in its group with exactly its fields`() = runTest {
        val fields = listOf(EntryField("Title", "Shop", false), EntryField("Password", "x", true))

        val id = repository.create(DB, GroupId(WORK), fields)

        assertEquals(fields, repository.read(DB, id).fields)
        assertEquals(listOf("Bank", "Mail", "Shop"), repository.entries(DB, GroupId(WORK)).map { it.title })
    }

    @Test
    fun `an entry created in a group that is gone - the group is not found`() = runTest {
        val failure = expectFailure<EntryFailure.GroupNotFound> {
            repository.create(DB, GroupId("missing"), listOf(EntryField("Title", "Shop", false)))
        }

        assertEquals("NotFound", (failure.cause as CoreException).kind)
    }

    @Test
    fun `an updated entry holds the new fields`() = runTest {
        val fields = listOf(EntryField("Title", "Bank", false), EntryField("PIN", "2", true))

        repository.update(DB, EntryId("bank"), fields)

        assertEquals(fields, repository.read(DB, EntryId("bank")).fields)
    }

    @Test
    fun `a moved entry leaves its group for the other one, its fields as they were`() = runTest {
        val before = repository.read(DB, EntryId("bank")).fields

        repository.move(DB, EntryId("bank"), GroupId(ROOT))

        assertEquals(EntryRecord(GroupId(ROOT), before), repository.read(DB, EntryId("bank")))
        assertEquals(listOf("Mail"), repository.entries(DB, GroupId(WORK)).map { it.title })
        assertEquals(listOf("Bank"), repository.entries(DB, GroupId(ROOT)).map { it.title })
    }

    @Test
    fun `an entry moved into a group that is gone - the group is not found`() = runTest {
        expectFailure<EntryFailure.GroupNotFound> { repository.move(DB, EntryId("bank"), GroupId("missing")) }
    }

    @Test
    fun `an entry that is gone is not found`() = runTest {
        expectFailure<EntryFailure.EntryNotFound> { repository.read(DB, EntryId("missing")) }
        expectFailure<EntryFailure.EntryNotFound> { repository.update(DB, EntryId("missing"), emptyList()) }
        expectFailure<EntryFailure.EntryNotFound> { repository.moveToRecycleBin(DB, EntryId("missing")) }
    }

    @Test
    fun `a deleted entry leaves its group for the recycle bin`() = runTest {
        repository.moveToRecycleBin(DB, EntryId("mail"))

        assertEquals(listOf("Bank"), repository.entries(DB, GroupId(WORK)).map { it.title })
        val bin = core.groupTrees.getValue(DB).groups.getValue(FakeNativeCore.RECYCLE_BIN_ID)
        assertEquals(listOf("mail"), bin.entryIds)
    }

    @Test
    fun `two fields under one key reaching the core are a bug of ours`() = runTest {
        val failure = expectFailure<EntryFailure.Unexpected> {
            repository.update(DB, EntryId("bank"), listOf(EntryField("PIN", "1", true), EntryField("PIN", "2", true)))
        }

        assertEquals("DuplicateFieldKey", (failure.cause as CoreException).kind)
    }

    @Test
    fun `any other failure of the core is unexpected`() = runTest {
        val cause = CoreException("DbKeyNotFound", "not open")
        core.failures["entries"] = cause

        val failure = expectFailure<EntryFailure.Unexpected> { repository.entries(DB, GroupId(WORK)) }

        assertSame(cause, failure.cause)
    }

    private fun data(key: String, value: String, protected: Boolean = false) = EntryFieldData(key, value, protected)

    private companion object {
        const val DB = "content://db"
        const val ROOT = "root"
        const val WORK = "work"
    }
}
