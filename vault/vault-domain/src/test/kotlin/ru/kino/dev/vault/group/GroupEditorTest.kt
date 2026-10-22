package ru.kino.dev.vault.group

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import ru.kino.dev.core.CoreException
import ru.kino.dev.core.MergeSummary
import ru.kino.dev.core.expectFailure
import ru.kino.dev.vault.VaultDatabaseRepository

class GroupEditorTest {

    private val journal = mutableListOf<String>()
    private val groups = RecordingGroups(journal)
    private val database = RecordingDatabase(journal)
    private val editor = GroupEditor(groups, database)

    @Test
    fun `creating makes the group with the name trimmed, then saves`() = runTest {
        val id = editor.create(DB, WORK, "  Bank ")

        assertEquals(GroupId("new"), id)
        assertEquals(listOf("tree", "create work Bank", "save"), journal)
    }

    @Test
    fun `a name a neighbour has is refused before anything changes`() = runTest {
        expectFailure<GroupFailure.NameTaken> { editor.create(DB, ROOT, "home") }

        assertEquals(listOf("tree"), journal)
    }

    @Test
    fun `an empty name is refused before anything changes`() = runTest {
        expectFailure<GroupFailure.NameEmpty> { editor.create(DB, ROOT, " ") }

        assertEquals(listOf("tree"), journal)
    }

    @Test
    fun `creating inside a group that is gone is refused`() = runTest {
        expectFailure<GroupFailure.GroupNotFound> { editor.create(DB, GroupId("missing"), "Bank") }
        expectFailure<GroupFailure.GroupNotFound> { editor.create(DB, OLD, "Bank") }
    }

    @Test
    fun `renaming changes the name, then saves`() = runTest {
        editor.rename(DB, WORK, " Office ")

        assertEquals(listOf("tree", "rename work Office", "save"), journal)
    }

    @Test
    fun `renaming to the same name is not a change and writes nothing`() = runTest {
        editor.rename(DB, WORK, " Work ")

        assertEquals(listOf("tree"), journal)
    }

    @Test
    fun `renaming to the own name in another case is a change`() = runTest {
        editor.rename(DB, WORK, "WORK")

        assertEquals(listOf("tree", "rename work WORK", "save"), journal)
    }

    @Test
    fun `renaming to the name of a neighbour is refused`() = runTest {
        expectFailure<GroupFailure.NameTaken> { editor.rename(DB, WORK, "Home") }

        assertEquals(listOf("tree"), journal)
    }

    @Test
    fun `deleting moves the group into the recycle bin, then saves`() = runTest {
        editor.delete(DB, WORK)

        assertEquals(listOf("tree", "recycle work", "save"), journal)
    }

    @Test
    fun `a group already in the recycle bin is not there to delete`() = runTest {
        expectFailure<GroupFailure.GroupNotFound> { editor.delete(DB, OLD) }
    }

    @Test
    fun `the root is not renamed or deleted by hand`() = runTest {
        expectFailure<GroupFailure.Unexpected> { editor.rename(DB, ROOT, "Other") }
        expectFailure<GroupFailure.Unexpected> { editor.delete(DB, ROOT) }

        assertEquals(listOf("tree", "tree"), journal)
    }

    @Test
    fun `a change that is not written is reported with what failed`() = runTest {
        val cause = CoreException("Io", "disk full")
        database.saveFailure = cause

        val failure = expectFailure<GroupFailure.NotSaved> { editor.delete(DB, WORK) }

        assertSame(cause, failure.cause)
        assertEquals(listOf("tree", "recycle work", "save"), journal)
    }

    @Test
    fun `a failure of the repository travels as it is`() = runTest {
        groups.failure = GroupFailure.GroupNotFound()

        expectFailure<GroupFailure.GroupNotFound> { editor.rename(DB, WORK, "Office") }
        assertEquals(listOf("tree", "rename work Office"), journal)
    }

    /** Answers with [sampleTree] and writes every change to the journal; changes nothing. */
    private class RecordingGroups(private val journal: MutableList<String>) : GroupRepository {

        var failure: GroupFailure? = null

        override suspend fun tree(dbKey: String): VaultTree {
            journal += "tree"
            return sampleTree()
        }

        override suspend fun create(dbKey: String, parentId: GroupId, name: String): GroupId {
            change("create ${parentId.value} $name")
            return GroupId("new")
        }

        override suspend fun rename(dbKey: String, groupId: GroupId, name: String) {
            change("rename ${groupId.value} $name")
        }

        override suspend fun delete(dbKey: String, groupId: GroupId) {
            change("recycle ${groupId.value}")
        }

        private fun change(step: String) {
            journal += step
            failure?.let { throw it }
        }
    }

    private class RecordingDatabase(private val journal: MutableList<String>) : VaultDatabaseRepository {

        var saveFailure: Exception? = null

        override suspend fun save(dbKey: String) {
            journal += "save"
            saveFailure?.let { throw it }
        }

        override suspend fun lock(dbKey: String) = error("not used")

        override suspend fun close(dbKey: String) = error("not used")

        override suspend fun hasChangedElsewhere(dbKey: String): Boolean = error("not used")

        override suspend fun merge(dbKey: String): MergeSummary = error("not used")
    }

    private companion object {
        const val DB = "content://db"
    }
}
