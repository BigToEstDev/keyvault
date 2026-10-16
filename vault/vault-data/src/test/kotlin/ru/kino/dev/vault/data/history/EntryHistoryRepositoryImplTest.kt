package ru.kino.dev.vault.data.history

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
import ru.kino.dev.vault.group.EntryId
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
