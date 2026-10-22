package ru.kino.dev.vault.data.group

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import ru.kino.dev.core.Compression
import ru.kino.dev.core.CoreException
import ru.kino.dev.core.DatabaseFile
import ru.kino.dev.core.FakeNativeCore
import ru.kino.dev.core.GroupNode
import ru.kino.dev.core.GroupTree
import ru.kino.dev.core.KdbxVersion
import ru.kino.dev.core.OpenedDatabase
import ru.kino.dev.core.expectFailure
import ru.kino.dev.vault.group.EntryId
import ru.kino.dev.vault.group.GroupFailure
import ru.kino.dev.vault.group.GroupId

class GroupRepositoryImplTest {

    private val core = FakeNativeCore()
    private val repository = GroupRepositoryImpl(core)

    init {
        core.opened[DB] = OpenedDatabase(DB, "Passwords", "passwords.kdbx", null)
        core.databaseFile = DatabaseFile(name = "Passwords", version = KdbxVersion.V41, compression = Compression.GZip)
        core.groupTrees[DB] = GroupTree(
            rootId = ROOT,
            recycleBinId = null,
            groups = mapOf(
                ROOT to GroupNode(ROOT, null, "Passwords", listOf(WORK), listOf("e1")),
                WORK to GroupNode(WORK, ROOT, "Work", emptyList(), listOf("e2", "e3")),
            ),
        )
    }

    @Test
    fun `the tree is read as the core has it`() = runTest {
        val tree = repository.tree(DB)

        assertEquals(GroupId(ROOT), tree.rootId)
        assertNull(tree.recycleBinId)
        assertEquals(listOf(GroupId(WORK)), tree.subgroups(GroupId(ROOT)).map { it.id })
        assertEquals(listOf(EntryId("e2"), EntryId("e3")), tree.group(GroupId(WORK))?.entryIds)
        assertEquals(3, tree.entryCount(GroupId(ROOT)).nested)
    }

    // Renamed in the settings: the root group keeps its own name in the file, the screens show the database's
    @Test
    fun `the root is named as the database`() = runTest {
        core.databaseFile = core.databaseFile.copy(name = "Home")

        val tree = repository.tree(DB)

        assertEquals("Home", tree.root.name)
        assertEquals("Work", tree.group(GroupId(WORK))?.name)
    }

    @Test
    fun `a database without a name keeps the name of its root group`() = runTest {
        core.databaseFile = core.databaseFile.copy(name = "")

        assertEquals("Passwords", repository.tree(DB).root.name)
    }

    @Test
    fun `a created group is in the next tree, inside its parent`() = runTest {
        val id = repository.create(DB, GroupId(WORK), "Mail")

        val tree = repository.tree(DB)
        assertEquals("Mail", tree.group(id)?.name)
        assertEquals(listOf(id), tree.subgroups(GroupId(WORK)).map { it.id })
    }

    @Test
    fun `a renamed group keeps its place`() = runTest {
        repository.rename(DB, GroupId(WORK), "Office")

        val tree = repository.tree(DB)
        assertEquals("Office", tree.group(GroupId(WORK))?.name)
        assertEquals(listOf(GroupId(WORK)), tree.subgroups(GroupId(ROOT)).map { it.id })
    }

    @Test
    fun `a deleted group leaves the tree, and its entries leave the count`() = runTest {
        repository.delete(DB, GroupId(WORK))

        val tree = repository.tree(DB)
        assertNull(tree.group(GroupId(WORK)))
        assertEquals(GroupId(FakeNativeCore.RECYCLE_BIN_ID), tree.recycleBinId)
        assertEquals(emptyList<GroupId>(), tree.subgroups(GroupId(ROOT)).map { it.id })
        assertEquals(1, tree.entryCount(GroupId(ROOT)).nested)
    }

    @Test
    fun `a group the core does not find is not found`() = runTest {
        val failure = expectFailure<GroupFailure.GroupNotFound> {
            repository.rename(DB, GroupId("missing"), "Office")
        }

        assertEquals("NotFound", (failure.cause as CoreException).kind)
    }

    @Test
    fun `any other failure of the core is unexpected, with the cause kept`() = runTest {
        val cause = CoreException("DbKeyNotFound", "not open")
        core.failures["groupTree"] = cause

        val failure = expectFailure<GroupFailure.Unexpected> { repository.tree(DB) }

        assertSame(cause, failure.cause)
    }

    @Test
    fun `a bug below is unexpected too`() = runTest {
        core.failures["createGroup"] = IllegalStateException("broken")

        expectFailure<GroupFailure.Unexpected> { repository.create(DB, GroupId(WORK), "Mail") }
    }

    private companion object {
        const val DB = "content://db"
        const val ROOT = "root"
        const val WORK = "work"
    }
}
