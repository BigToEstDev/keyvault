package ru.kino.dev.vault.entry

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import ru.kino.dev.core.CoreException
import ru.kino.dev.core.MergeSummary
import ru.kino.dev.core.expectFailure
import ru.kino.dev.vault.VaultDatabaseRepository
import ru.kino.dev.vault.entry.StandardPreset.PASSWORD
import ru.kino.dev.vault.entry.StandardPreset.TITLE
import ru.kino.dev.vault.entry.StandardPreset.USER_NAME
import ru.kino.dev.vault.group.EntryId
import ru.kino.dev.vault.group.GroupId

class EntryEditorTest {

    private val journal = mutableListOf<String>()
    private val entries = FakeEntryRepository(journal)
    private val database = RecordingDatabase(journal)
    private val editor = EntryEditor(entries, database)

    private val filled = StandardPreset.preset.blank()
        .withValue(TITLE, "Mail")
        .withValue(USER_NAME, "anton")
        .withValue(PASSWORD, "secret")

    @Test
    fun `creating makes the entry with the fields in the order of the form, then saves`() = runTest {
        val id = editor.create(DB, GROUP, filled.copy(ownFields = listOf(field("  PIN "))))

        assertEquals(listOf("create", "save"), journal)
        assertEquals(
            listOf(TITLE, USER_NAME, PASSWORD, "URL", "Notes", "PIN"),
            entries.stored.getValue(id).fields.map { it.key },
        )
        assertEquals(GROUP, entries.stored.getValue(id).groupId)
    }

    @Test
    fun `a new entry without its required fields is refused with all of them, before anything changes`() = runTest {
        val failure = expectFailure<EntryFailure.InvalidFields> {
            editor.create(DB, GROUP, StandardPreset.preset.blank().withValue(TITLE, "Mail"))
        }

        assertEquals(listOf(FieldProblem.Required(USER_NAME), FieldProblem.Required(PASSWORD)), failure.problems)
        assertEquals(emptyList<String>(), journal)
    }

    @Test
    fun `changing an entry replaces its fields, then saves`() = runTest {
        val id = entries.add("e1", GROUP, filled.fields())

        editor.update(DB, id, filled.withValue(PASSWORD, "new secret"))

        assertEquals(listOf("fields", "update", "save"), journal)
        assertEquals("new secret", entries.stored.getValue(id).fields.first { it.key == PASSWORD }.value)
    }

    @Test
    fun `the same fields again are not a change and write nothing`() = runTest {
        val id = entries.add("e1", GROUP, filled.fields())

        editor.update(DB, id, filled)

        assertEquals(listOf("fields"), journal)
    }

    @Test
    fun `a foreign entry without a user name is saved without one`() = runTest {
        val foreign = filled.withValue(USER_NAME, "")
        val id = entries.add("e1", GROUP, foreign.fields())

        editor.update(DB, id, foreign.withValue(TITLE, "Bank"))

        assertEquals(listOf("fields", "update", "save"), journal)
    }

    @Test
    fun `a password cannot be emptied`() = runTest {
        val id = entries.add("e1", GROUP, filled.fields())

        val failure = expectFailure<EntryFailure.InvalidFields> { editor.update(DB, id, filled.withValue(PASSWORD, "")) }

        assertEquals(listOf(FieldProblem.Required(PASSWORD)), failure.problems)
        assertEquals(listOf("fields"), journal)
    }

    @Test
    fun `two fields of another program under one key - saved once one is renamed, with its spaces kept`() = runTest {
        val read = filled.fields() + field(" PIN") + field(" PIN")
        val id = entries.add("e1", GROUP, read)
        val content = entries.content(DB, id)
        journal.clear()

        // Unchanged it is not saved at all; any other change asks for the twin to be renamed first
        editor.update(DB, id, content)
        assertEquals(listOf("fields"), journal)
        val failure = expectFailure<EntryFailure.InvalidFields> { editor.update(DB, id, content.withValue(TITLE, "Bank")) }
        assertEquals(listOf(FieldProblem.BadName(1, NameProblem.TAKEN)), failure.problems)

        editor.update(DB, id, content.copy(ownFields = listOf(field(" PIN"), field(" Card "))))
        assertEquals(listOf(" PIN", "Card"), entries.stored.getValue(id).fields.drop(5).map { it.key })
    }

    @Test
    fun `deleting moves the entry into the recycle bin, then saves`() = runTest {
        val id = entries.add("e1", GROUP, filled.fields())

        editor.delete(DB, id)

        assertEquals(listOf("moveToRecycleBin", "save"), journal)
        assertEquals(listOf(id), entries.recycled)
    }

    @Test
    fun `an entry that is gone is not found`() = runTest {
        expectFailure<EntryFailure.EntryNotFound> { editor.update(DB, EntryId("missing"), filled) }
        expectFailure<EntryFailure.EntryNotFound> { editor.delete(DB, EntryId("missing")) }
    }

    @Test
    fun `a change that is not written is reported with what failed`() = runTest {
        val cause = CoreException("Io", "disk full")
        database.saveFailure = cause

        val failure = expectFailure<EntryFailure.NotSaved> { editor.create(DB, GROUP, filled) }

        assertSame(cause, failure.cause)
        assertEquals(listOf("create", "save"), journal)
    }

    private fun field(key: String, value: String = "v") = EntryField(key, value, protected = false)

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
        val GROUP = GroupId("work")
    }
}
