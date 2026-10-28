package ru.kino.dev.vault.data.history

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlinx.coroutines.test.runTest
import ru.kino.dev.core.CoreException
import ru.kino.dev.core.EntryFieldData
import ru.kino.dev.core.FakeNativeCore
import ru.kino.dev.core.GroupNode
import ru.kino.dev.core.GroupTree
import ru.kino.dev.core.expectFailure
import ru.kino.dev.vault.entry.EntryFailure
import ru.kino.dev.vault.entry.EntryField
import ru.kino.dev.vault.group.EntryId
import ru.kino.dev.vault.group.GroupId
import ru.kino.dev.vault.history.HistoryVersion

class EntryHistoryRepositoryImplTest {

    private val core = FakeNativeCore()
    private val repository = EntryHistoryRepositoryImpl(core)

    init {
        core.groupTrees[DB] = GroupTree(
            rootId = ROOT,
            recycleBinId = null,
            groups = mapOf(ROOT to GroupNode(ROOT, null, "Passwords", emptyList(), emptyList())),
        )
        core.addEntry(DB, ROOT, "mail", listOf(EntryFieldData("Title", "Mail", false)), createdAt = 1_000L)
    }

    @Test
    fun `every update leaves the state before it in the history, oldest first`() = runTest {
        core.updateEntry(DB, "mail", listOf(EntryFieldData("Title", "Mail 2", false)))
        core.updateEntry(DB, "mail", listOf(EntryFieldData("Title", "Mail 3", false)))

        val versions = repository.versions(DB, EntryId("mail"))

        assertEquals(listOf(0, 1), versions.map { it.index })
        assertEquals(HistoryVersion(0, 1_000L), versions.first())
    }

    @Test
    fun `a version is read as the entry was then, with when it was saved and the group of the entry`() = runTest {
        core.updateEntry(DB, "mail", listOf(EntryFieldData("Title", "Post", false)))

        val version = repository.version(DB, EntryId("mail"), 0)

        assertEquals(listOf(EntryField("Title", "Mail", false)), version.fields)
        assertEquals(GroupId(ROOT), version.groupId)
        assertEquals(1_000L, version.times.modified)
    }

    @Test
    fun `a version restored is the entry again, and the state before is in the history`() = runTest {
        core.updateEntry(DB, "mail", listOf(EntryFieldData("Title", "Post", false)))

        repository.restore(DB, EntryId("mail"), 0)

        assertEquals(listOf(EntryFieldData("Title", "Mail", false)), core.entry(DB, "mail").fields)
        assertEquals(2, repository.versions(DB, EntryId("mail")).size)
    }

    @Test
    fun `a version deleted leaves the history`() = runTest {
        core.updateEntry(DB, "mail", listOf(EntryFieldData("Title", "Post", false)))

        repository.delete(DB, EntryId("mail"), 0)

        assertEquals(emptyList<HistoryVersion>(), repository.versions(DB, EntryId("mail")))
    }

    @Test
    fun `the whole history deleted, the entry stays`() = runTest {
        core.updateEntry(DB, "mail", listOf(EntryFieldData("Title", "Post", false)))

        repository.deleteAll(DB, EntryId("mail"))

        assertEquals(emptyList<HistoryVersion>(), repository.versions(DB, EntryId("mail")))
        assertEquals(listOf(EntryFieldData("Title", "Post", false)), core.entry(DB, "mail").fields)
    }

    @Test
    fun `deleting the history of an entry that is not there is EntryNotFound`() = runTest {
        expectFailure<EntryFailure.EntryNotFound> { repository.deleteAll(DB, EntryId("missing")) }
    }

    @Test
    fun `a version that is not there is EntryNotFound - the core does not say which is missing`() = runTest {
        expectFailure<EntryFailure.EntryNotFound> { repository.restore(DB, EntryId("mail"), 5) }
        expectFailure<EntryFailure.EntryNotFound> { repository.delete(DB, EntryId("mail"), 5) }
    }

    @Test
    fun `the history of an entry that is not there is EntryNotFound`() = runTest {
        val failure = expectFailure<EntryFailure.EntryNotFound> { repository.versions(DB, EntryId("missing")) }

        assertEquals("NotFound", (failure.cause as CoreException).kind)
    }

    @Test
    fun `any other failure of the core is Unexpected, with the cause`() = runTest {
        val cause = CoreException("DbKeyNotFound", "not open")
        core.failures["entryHistory"] = cause

        val failure = expectFailure<EntryFailure.Unexpected> { repository.versions(DB, EntryId("mail")) }

        assertSame(cause, failure.cause)
    }

    private companion object {
        const val DB = "content://db"
        const val ROOT = "root"
    }
}
