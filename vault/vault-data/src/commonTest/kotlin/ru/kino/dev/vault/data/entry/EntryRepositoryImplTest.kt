package ru.kino.dev.vault.data.entry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import ru.kino.dev.core.CoreException
import ru.kino.dev.core.EntryFieldData
import ru.kino.dev.core.FakeNativeCore
import ru.kino.dev.core.GroupNode
import ru.kino.dev.core.GroupTree
import ru.kino.dev.core.expectFailure
import ru.kino.dev.vault.attachment.AttachmentsEdit
import ru.kino.dev.vault.attachment.NewAttachment
import ru.kino.dev.vault.data.changes.VaultChangesImpl
import ru.kino.dev.vault.entry.EntryFailure
import ru.kino.dev.vault.entry.EntryField
import ru.kino.dev.vault.entry.EntryRecord
import ru.kino.dev.vault.entry.EntryTimes
import ru.kino.dev.vault.group.EntryId
import ru.kino.dev.vault.group.GroupId

class EntryRepositoryImplTest {

    private val core = FakeNativeCore()
    private val changes = VaultChangesImpl()
    private val repository = EntryRepositoryImpl(core, changes)

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
            EntryRecord(
                GroupId(WORK),
                listOf(EntryField("Title", "Bank", false), EntryField("PIN", "1", true)),
                EntryTimes(created = 1_700_000_002L, modified = 1_700_000_002L),
            ),
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
    fun `an updated entry holds the new time of its change, its creation as it was`() = runTest {
        val before = repository.read(DB, EntryId("bank")).times

        repository.update(DB, EntryId("bank"), listOf(EntryField("Title", "Bank", false)))

        val after = repository.read(DB, EntryId("bank")).times
        assertEquals(before.created, after.created)
        assertTrue(checkNotNull(after.modified) > checkNotNull(before.modified))
    }

    @Test
    fun `an updated entry holds the new fields`() = runTest {
        val fields = listOf(EntryField("Title", "Bank", false), EntryField("PIN", "2", true))

        repository.update(DB, EntryId("bank"), fields)

        assertEquals(fields, repository.read(DB, EntryId("bank")).fields)
    }

    @Test
    fun `a moved entry leaves its group for the other one, its fields as they were`() = runTest {
        val before = repository.read(DB, EntryId("bank"))

        repository.move(DB, EntryId("bank"), GroupId(ROOT))

        assertEquals(before.copy(groupId = GroupId(ROOT)), repository.read(DB, EntryId("bank")))
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
        expectFailure<EntryFailure.EntryNotFound> { repository.delete(DB, EntryId("missing")) }
    }

    @Test
    fun `a deleted entry leaves its group for the recycle bin`() = runTest {
        repository.delete(DB, EntryId("mail"))

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

    @Test
    fun `every write moves the version of the database on, reading does not`() = runTest {
        repository.entries(DB, GroupId(WORK))
        repository.read(DB, EntryId("mail"))
        assertEquals(0, changes.current(DB))

        val id = repository.create(DB, GroupId(WORK), listOf(EntryField("Title", "New", false)))
        repository.update(DB, id, listOf(EntryField("Title", "Newer", false)))
        repository.move(DB, id, GroupId(ROOT))
        repository.delete(DB, id)

        assertEquals(4, changes.current(DB))
    }

    @Test
    fun `a write the core refused does not move the version`() = runTest {
        expectFailure<EntryFailure.EntryNotFound> { repository.delete(DB, EntryId("missing")) }

        assertEquals(0, changes.current(DB))
    }

    @Test
    fun `files made with an entry come back with it, by name and size`() = runTest {
        val id = repository.create(DB, GroupId(WORK), listOf(field("Title", "Mail")), listOf(NewAttachment("a.txt", byteArrayOf(1, 2))))

        val attachments = repository.read(DB, id).attachments

        assertEquals(listOf("a.txt" to 2L), attachments.map { it.name to it.size })
        assertEquals(listOf(1, 2), core.attachmentBytes.getValue(attachments.single().id).map { it.toInt() })
    }

    @Test
    fun `an update keeps the attachments named and adds the files after them`() = runTest {
        val first = listOf(NewAttachment("a.txt", byteArrayOf(1)), NewAttachment("b.txt", byteArrayOf(2)))
        val id = repository.create(DB, GroupId(WORK), listOf(field("Title", "Mail")), first)
        val (_, b) = repository.read(DB, id).attachments

        repository.update(DB, id, listOf(field("Title", "Mail")), AttachmentsEdit(listOf(b), listOf(NewAttachment("c.txt", byteArrayOf(3)))))

        assertEquals(listOf("b.txt", "c.txt"), repository.read(DB, id).attachments.map { it.name })
    }

    @Test
    fun `an update without an edit of the attachments keeps them`() = runTest {
        val id = repository.create(DB, GroupId(WORK), listOf(field("Title", "Mail")), listOf(NewAttachment("a.txt", byteArrayOf(1))))

        repository.update(DB, id, listOf(field("Title", "Post")))

        assertEquals(listOf("a.txt"), repository.read(DB, id).attachments.map { it.name })
    }

    private fun field(key: String, value: String) = EntryField(key, value, protected = false)

    private fun data(key: String, value: String, protected: Boolean = false) = EntryFieldData(key, value, protected)

    private companion object {
        const val DB = "content://db"
        const val ROOT = "root"
        const val WORK = "work"
    }
}
