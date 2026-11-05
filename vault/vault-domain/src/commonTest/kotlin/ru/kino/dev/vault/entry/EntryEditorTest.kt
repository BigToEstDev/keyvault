package ru.kino.dev.vault.entry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlinx.coroutines.test.runTest
import ru.kino.dev.core.CoreException
import ru.kino.dev.core.FieldProtection
import ru.kino.dev.core.MergeSummary
import ru.kino.dev.core.expectFailure
import ru.kino.dev.core.preset.CustomPreset
import ru.kino.dev.core.preset.FakePresetRepository
import ru.kino.dev.core.preset.PresetId
import ru.kino.dev.core.preset.PresetLink
import ru.kino.dev.core.preset.StandardPreset
import ru.kino.dev.core.preset.StandardPreset.PASSWORD
import ru.kino.dev.core.preset.StandardPreset.TITLE
import ru.kino.dev.core.preset.StandardPreset.USER_NAME
import ru.kino.dev.vault.VaultDatabaseRepository
import ru.kino.dev.vault.attachment.ATTACHMENTS_MAX_PER_ENTRY
import ru.kino.dev.vault.attachment.ATTACHMENT_MAX_BYTES
import ru.kino.dev.vault.attachment.AttachmentsEdit
import ru.kino.dev.vault.attachment.EntryAttachment
import ru.kino.dev.vault.attachment.NewAttachment
import ru.kino.dev.vault.entries.EntrySettingsFailure
import ru.kino.dev.vault.entries.FakeEntrySettingsRepository
import ru.kino.dev.vault.group.EntryId
import ru.kino.dev.vault.group.GroupId

class EntryEditorTest {

    private val journal = mutableListOf<String>()
    private val entries = FakeEntryRepository(journal)
    private val database = RecordingDatabase(journal)
    private val card = CustomPreset.of(PresetId("card"), "Card", listOf(listOf("PIN")))
    private val opener = EntryOpener(entries, FakePresetRepository(card))
    private val settings = FakeEntrySettingsRepository()
    private val editor = EntryEditor(entries, opener, settings, database)

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
    fun `a new entry is private where the database says so, whatever the preset`() = runTest {
        settings.entries = settings.entries.copy(protection = FieldProtection.DEFAULT.copy(title = true, url = true))

        val id = editor.create(DB, GROUP, filled.withValue("URL", "mail.example"))

        val protected = entries.stored.getValue(id).fields.filter { it.protected }.map { it.key }
        assertEquals(listOf(TITLE, PASSWORD, "URL"), protected)
    }

    @Test
    fun `a field the database leaves open stays as the preset says`() = runTest {
        settings.entries = settings.entries.copy(protection = FieldProtection.DEFAULT.copy(password = false))

        val id = editor.create(DB, GROUP, filled)

        val protected = entries.stored.getValue(id).fields.filter { it.protected }.map { it.key }
        assertEquals(listOf(PASSWORD), protected)
    }

    @Test
    fun `the protection of the database not read - nothing is created`() = runTest {
        settings.failure = EntrySettingsFailure.Unexpected()

        expectFailure<EntryFailure.Unexpected> { editor.create(DB, GROUP, filled) }

        assertEquals(emptyList<String>(), journal)
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

        editor.update(DB, id, filled.withValue(PASSWORD, "new secret"), GROUP)

        assertEquals(listOf("read", "update", "save"), journal)
        assertEquals("new secret", entries.stored.getValue(id).fields.first { it.key == PASSWORD }.value)
    }

    @Test
    fun `the same fields again are not a change and write nothing`() = runTest {
        val id = entries.add("e1", GROUP, filled.fields())

        editor.update(DB, id, filled, GROUP)

        assertEquals(listOf("read"), journal)
    }

    @Test
    fun `a foreign entry without a user name is saved without one`() = runTest {
        val foreign = filled.withValue(USER_NAME, "")
        val id = entries.add("e1", GROUP, foreign.fields())

        editor.update(DB, id, foreign.withValue(TITLE, "Bank"), GROUP)

        assertEquals(listOf("read", "update", "save"), journal)
    }

    @Test
    fun `a password cannot be emptied`() = runTest {
        val id = entries.add("e1", GROUP, filled.fields())

        val failure = expectFailure<EntryFailure.InvalidFields> {
            editor.update(DB, id, filled.withValue(PASSWORD, ""), GROUP)
        }

        assertEquals(listOf(FieldProblem.Required(PASSWORD)), failure.problems)
        assertEquals(listOf("read"), journal)
    }

    @Test
    fun `two fields of another program under one key - saved once one is renamed, with its spaces kept`() = runTest {
        val read = filled.fields() + field(" PIN") + field(" PIN")
        val id = entries.add("e1", GROUP, read)
        val content = opener.open(DB, id).content
        journal.clear()

        // Unchanged it is not saved at all; any other change asks for the twin to be renamed first
        editor.update(DB, id, content, GROUP)
        assertEquals(listOf("read"), journal)
        val failure = expectFailure<EntryFailure.InvalidFields> {
            editor.update(DB, id, content.withValue(TITLE, "Bank"), GROUP)
        }
        assertEquals(listOf(FieldProblem.BadName(1, NameProblem.TAKEN)), failure.problems)

        editor.update(DB, id, content.copy(ownFields = listOf(field(" PIN"), field(" Card "))), GROUP)
        assertEquals(listOf(" PIN", "Card"), entries.stored.getValue(id).fields.drop(5).map { it.key })
    }

    @Test
    fun `an entry of a preset of one's own is saved with its link, switched to the standard one - without`() = runTest {
        val id = editor.create(DB, GROUP, EntryDraft(filled).withPreset(card).content.withValue("PIN", "1234"))
        assertEquals(field(PresetLink.KEY, "card"), entries.stored.getValue(id).fields.last())

        val read = opener.open(DB, id).content
        assertEquals(card, read.preset)
        editor.update(DB, id, EntryDraft(read).withPreset(StandardPreset.preset).content, GROUP)

        assertFalse(entries.stored.getValue(id).fields.any { it.key == PresetLink.KEY || it.key == "PIN" })
    }

    @Test
    fun `another group alone moves the entry and saves, the fields not written`() = runTest {
        val id = entries.add("e1", GROUP, filled.fields())

        editor.update(DB, id, filled, OTHER_GROUP)

        assertEquals(listOf("read", "move", "save"), journal)
        assertEquals(OTHER_GROUP, entries.stored.getValue(id).groupId)
    }

    @Test
    fun `other fields and another group - both, and one save`() = runTest {
        val id = entries.add("e1", GROUP, filled.fields())

        editor.update(DB, id, filled.withValue(PASSWORD, "new secret"), OTHER_GROUP)

        assertEquals(listOf("read", "update", "move", "save"), journal)
        assertEquals(OTHER_GROUP, entries.stored.getValue(id).groupId)
    }

    @Test
    fun `an emptied password is refused before the entry moves`() = runTest {
        val id = entries.add("e1", GROUP, filled.fields())

        expectFailure<EntryFailure.InvalidFields> {
            editor.update(DB, id, filled.withValue(PASSWORD, ""), OTHER_GROUP)
        }

        assertEquals(listOf("read"), journal)
        assertEquals(GROUP, entries.stored.getValue(id).groupId)
    }

    @Test
    fun `deleting moves the entry into the recycle bin, then saves`() = runTest {
        val id = entries.add("e1", GROUP, filled.fields())

        editor.delete(DB, id)

        assertEquals(listOf("delete", "save"), journal)
        assertEquals(listOf(id), entries.recycled)
    }

    @Test
    fun `an entry that is gone is not found`() = runTest {
        expectFailure<EntryFailure.EntryNotFound> { editor.update(DB, EntryId("missing"), filled, GROUP) }
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

    @Test
    fun `a new entry takes the files added, in their order, with the same write`() = runTest {
        val id = editor.create(DB, GROUP, filled, listOf(file("a.txt"), file("b.png")))

        assertEquals(listOf("create", "save"), journal)
        assertEquals(listOf("a.txt", "b.png"), entries.stored.getValue(id).attachments.map { it.name })
    }

    @Test
    fun `two files under one name are refused before anything changes`() = runTest {
        expectFailure<EntryFailure.Unexpected> { editor.create(DB, GROUP, filled, listOf(file("a.txt"), file("a.txt"))) }

        assertEquals(emptyList<String>(), journal)
    }

    @Test
    fun `a file larger than an attachment may be is refused before anything changes`() = runTest {
        val large = NewAttachment("big.bin", ByteArray(ATTACHMENT_MAX_BYTES + 1))

        expectFailure<EntryFailure.Unexpected> { editor.create(DB, GROUP, filled, listOf(large)) }

        assertEquals(emptyList<String>(), journal)
    }

    @Test
    fun `attachments alone changed are a change - written and saved, the fields as they were`() = runTest {
        val id = withAttachments("a.txt", "b.png")
        val (a, _) = entries.stored.getValue(id).attachments

        editor.update(DB, id, filled, GROUP, AttachmentsEdit(kept = listOf(a), added = listOf(file("c.pdf"))))

        assertEquals(listOf("read", "update", "save"), journal)
        val entry = entries.stored.getValue(id)
        assertEquals(listOf("a.txt", "c.pdf"), entry.attachments.map { it.name })
        assertEquals(filled.fields(), entry.fields)
    }

    @Test
    fun `the same attachments again are not a change and write nothing`() = runTest {
        val id = withAttachments("a.txt", "b.png")
        val kept = entries.stored.getValue(id).attachments

        editor.update(DB, id, filled, GROUP, AttachmentsEdit(kept = kept, added = emptyList()))

        assertEquals(listOf("read"), journal)
    }

    @Test
    fun `the attachments kept in another order are a change`() = runTest {
        val id = withAttachments("a.txt", "b.png")
        val kept = entries.stored.getValue(id).attachments.reversed()

        editor.update(DB, id, filled, GROUP, AttachmentsEdit(kept = kept, added = emptyList()))

        assertEquals(listOf("b.png", "a.txt"), entries.stored.getValue(id).attachments.map { it.name })
    }

    @Test
    fun `no edit of the attachments keeps them while the fields change`() = runTest {
        val id = withAttachments("a.txt")

        editor.update(DB, id, filled.withValue(PASSWORD, "new secret"), GROUP)

        assertEquals(listOf("a.txt"), entries.stored.getValue(id).attachments.map { it.name })
    }

    @Test
    fun `the same bytes under two names are kept apart`() = runTest {
        val id = entries.add("e1", GROUP, filled.fields())
        entries.attachmentBytes["h"] = byteArrayOf(1)
        val twins = listOf(EntryAttachment("h", "a.txt", 1), EntryAttachment("h", "a (2).txt", 1))
        entries.stored[id] = entries.stored.getValue(id).copy(attachments = twins)

        editor.update(DB, id, filled, GROUP, AttachmentsEdit(kept = listOf(twins[1]), added = emptyList()))

        assertEquals(listOf("a (2).txt"), entries.stored.getValue(id).attachments.map { it.name })
    }

    @Test
    fun `a file added under a name the entry keeps is refused before anything changes`() = runTest {
        val id = withAttachments("a.txt")
        val kept = entries.stored.getValue(id).attachments

        expectFailure<EntryFailure.Unexpected> {
            editor.update(DB, id, filled, GROUP, AttachmentsEdit(kept = kept, added = listOf(file("a.txt"))))
        }

        assertEquals(listOf("read"), journal)
    }

    @Test
    fun `a file added past the limit of attachments is refused before anything changes`() = runTest {
        val id = withAttachments(*names(ATTACHMENTS_MAX_PER_ENTRY))
        val kept = entries.stored.getValue(id).attachments

        expectFailure<EntryFailure.Unexpected> {
            editor.update(DB, id, filled, GROUP, AttachmentsEdit(kept = kept, added = listOf(file("one more.txt"))))
        }

        assertEquals(listOf("read"), journal)
    }

    @Test
    fun `an entry of another program past the limit is saved as it is - one removed too`() = runTest {
        val id = withAttachments(*names(ATTACHMENTS_MAX_PER_ENTRY + 2))
        val kept = entries.stored.getValue(id).attachments.drop(1)

        editor.update(DB, id, filled, GROUP, AttachmentsEdit(kept = kept, added = emptyList()))

        assertEquals(ATTACHMENTS_MAX_PER_ENTRY + 1, entries.stored.getValue(id).attachments.size)
    }

    private fun names(count: Int) = Array(count) { "file $it.txt" }

    /** An entry of [filled] with files of [names], made through the fake and the journal cleared. */
    private suspend fun withAttachments(vararg names: String): EntryId {
        val id = entries.create(DB, GROUP, filled.fields(), names.map { file(it) })
        journal.clear()
        return id
    }

    private fun file(name: String) = NewAttachment(name, byteArrayOf(1, 2))

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
        val OTHER_GROUP = GroupId("home")
    }
}
