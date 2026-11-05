package ru.kino.dev.vault.pres.entryedit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.orbitmvi.orbit.test.testWithInternalState
import ru.kino.dev.core.CoreException
import ru.kino.dev.core.FakeDatabaseFiles
import ru.kino.dev.core.FieldProtection
import ru.kino.dev.core.FieldType
import ru.kino.dev.core.phone.FakePhoneNumbers
import ru.kino.dev.core.phone.PhoneCountry
import ru.kino.dev.core.phone.PhoneDefaults
import ru.kino.dev.core.preset.CustomPreset
import ru.kino.dev.core.preset.FakePresetRepository
import ru.kino.dev.core.preset.PresetId
import ru.kino.dev.core.preset.PresetLink
import ru.kino.dev.core.preset.StandardPreset
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.PresetEditRoute
import ru.kino.dev.vault.FakeVaultDatabase
import ru.kino.dev.vault.attachment.ATTACHMENTS_MAX_PER_ENTRY
import ru.kino.dev.vault.attachment.ATTACHMENT_MAX_BYTES
import ru.kino.dev.vault.attachment.AttachmentFiles
import ru.kino.dev.vault.attachment.EntryAttachment
import ru.kino.dev.vault.attachment.FakeAttachmentRepository
import ru.kino.dev.vault.entries.EntrySettingsFailure
import ru.kino.dev.vault.entries.FakeEntrySettingsRepository
import ru.kino.dev.vault.entry.EntryEditor
import ru.kino.dev.vault.entry.EntryFailure
import ru.kino.dev.vault.entry.EntryField
import ru.kino.dev.vault.entry.EntryOpener
import ru.kino.dev.vault.entry.FakeEntryRepository
import ru.kino.dev.vault.entry.FieldProblem
import ru.kino.dev.vault.entry.NameProblem
import ru.kino.dev.vault.entry.blank
import ru.kino.dev.vault.generator.FakePasswordGenerator
import ru.kino.dev.vault.group.EntryId
import ru.kino.dev.vault.group.FakeGroupRepository
import ru.kino.dev.vault.group.GroupId
import ru.kino.dev.vault.pres.common.GroupChoice
import ru.kino.dev.vault.pres.navigation.EntryCreateRoute
import ru.kino.dev.vault.pres.navigation.EntryReadRoute
import ru.kino.dev.vault.pres.testing.DB
import ru.kino.dev.vault.pres.testing.RecordingLog
import ru.kino.dev.vault.pres.testing.recordNavigation
import ru.kino.dev.vault.pres.testing.runEach
import ru.kino.dev.vault.trash.FakeTrashRepository

class EntryEditProcessorTest {

    private val entries = FakeEntryRepository()
    private val card = CustomPreset.of(PresetId("card"), "Card", listOf(listOf("PIN"), listOf("Bank")))
    private val presets = FakePresetRepository(card)
    private val opener = EntryOpener(entries, presets)
    private val group = GroupId("root")
    private val mail = entries.add(
        "mail",
        group,
        listOf(
            field(StandardPreset.TITLE, "Mail"),
            field(StandardPreset.USER_NAME, "me"),
            field(StandardPreset.PASSWORD, "secret", protected = true),
            field(StandardPreset.URL, ""),
            field(StandardPreset.NOTES, ""),
            field("PIN", "1234", protected = true),
            field("Recovery", "words"),
        ),
    )

    private val groups = FakeGroupRepository()
    private val work = groups.add("work", groups.rootId, "Work")
    private val database = FakeVaultDatabase()
    private val log = RecordingLog()
    private val settings = FakeEntrySettingsRepository()
    private val trash = FakeTrashRepository()
    private val phones = FakePhoneNumbers()
    private val phoneDefaults = PhoneDefaults { "RU" }
    private val files = FakeDatabaseFiles()
    private val generator = FakePasswordGenerator(listOf("Gen-1!", "Gen-2!"))

    // The generator (pass-docs, Step 104)

    @Test
    fun `the dice fills an empty password of a new entry and shows it`() = runTest {
        val (state, _) = creating({ onGeneratePassword() })

        assertEquals("Gen-1!", state.content?.value(StandardPreset.PASSWORD))
        assertTrue(state.passwordVisible)
        assertFalse(state.generatingPassword)
        assertTrue(state.changed)
    }

    @Test
    fun `the dice replaces the password of an entry unasked`() = runTest {
        val (state, _) = editing({ onGeneratePassword() })

        assertEquals("Gen-1!", state.content?.value(StandardPreset.PASSWORD))
        assertNull(state.dialog)
    }

    @Test
    fun `the dice again gives another password`() = runTest {
        val (state, _) = editing({ onGeneratePassword() }, { onGeneratePassword() })

        assertEquals("Gen-2!", state.content?.value(StandardPreset.PASSWORD))
    }

    @Test
    fun `a password generated takes the mark of a required field away`() = runTest {
        val (state, _) = creating({ onValueChange(StandardPreset.TITLE, "Bank") }, { onSave() }, { onGeneratePassword() })

        assertFalse(state.isMissing(StandardPreset.PASSWORD))
    }

    @Test
    fun `the generator refusing says so, the password as it was`() = runTest {
        generator.failure = EntryFailure.Unexpected(IllegalStateException("no"))

        val (state, _) = editing({ onGeneratePassword() })

        assertEquals("secret", state.content?.value(StandardPreset.PASSWORD))
        assertEquals(EntryEditMessage.Unexpected, state.message)
        assertEquals(1, log.logged.size)
    }

    // Attachments (pass-docs, Step 103)

    @Test
    fun `the attachments of the entry open on the form, kept, no change yet`() = runTest {
        withNotes()

        val (state, _) = editing()

        assertEquals(listOf("notes.txt"), state.attachments.map { it.name })
        assertFalse(state.changed)
    }

    @Test
    fun `add asks for the file - nothing is on the form before it is picked`() = runTest {
        val (state, _) = editing({ onAddAttachment() })

        assertTrue(state.attachments.isEmpty())
        assertFalse(state.addingAttachment)
    }

    @Test
    fun `a file picked goes after the attachments, a name taken numbered - a change`() = runTest {
        withNotes()
        picked("notes.txt", byteArrayOf(9))

        val (state, _) = editing({ onAttachmentPicked(PICKED) })

        assertEquals(listOf("notes.txt", "notes (2).txt"), state.attachments.map { it.name })
        assertTrue(state.changed)
    }

    @Test
    fun `a file larger than the limit is refused with the message`() = runTest {
        picked("video.mp4", ByteArray(ATTACHMENT_MAX_BYTES + 1))

        val (state, _) = editing({ onAttachmentPicked(PICKED) })

        assertTrue(state.attachments.isEmpty())
        assertEquals(EntryEditMessage.AttachmentTooLarge, state.message)
    }

    @Test
    fun `a file that cannot be read says so`() = runTest {
        files.unreachable += PICKED

        val (state, _) = editing({ onAttachmentPicked(PICKED) })

        assertEquals(EntryEditMessage.AttachmentNotRead, state.message)
        assertTrue(log.logged.isEmpty())
    }

    @Test
    fun `an attachment of the entry removed is off the form - a change`() = runTest {
        withNotes()

        val (state, _) = editing({ onRemoveAttachment(0) })

        assertTrue(state.attachments.isEmpty())
        assertTrue(state.changed)
    }

    @Test
    fun `a file added and removed again leaves the form as it opened`() = runTest {
        picked("a.txt", byteArrayOf(1, 2))

        val (state, _) = editing({ onAttachmentPicked(PICKED) }, { onRemoveAttachment(0) })

        assertTrue(state.attachments.isEmpty())
        assertFalse(state.changed)
    }

    @Test
    fun `saved, the entry has the attachments kept and the files added after them`() = runTest {
        withNotes()
        picked("photo.jpg", byteArrayOf(5))

        editing({ onAttachmentPicked(PICKED) }, { onSave() })

        assertEquals(listOf("notes.txt", "photo.jpg"), entries.stored.getValue(mail).attachments.map { it.name })
        assertEquals(listOf("save"), database.calls)
    }

    @Test
    fun `saved with an attachment removed, the entry is without it`() = runTest {
        withNotes()

        editing({ onRemoveAttachment(0) }, { onSave() })

        assertTrue(entries.stored.getValue(mail).attachments.isEmpty())
    }

    @Test
    fun `a new entry is made with the files added`() = runTest {
        picked("a.txt", byteArrayOf(1))

        creating(*filled(), { onAttachmentPicked(PICKED) }, { onSave() })

        val created = entries.stored.values.last()
        assertEquals(listOf("a.txt"), created.attachments.map { it.name })
    }

    @Test
    fun `back with a file added asks before leaving`() = runTest {
        picked("a.txt", byteArrayOf(1))

        val (state, events) = editing({ onAttachmentPicked(PICKED) }, { onBack() })

        assertEquals(EntryEditDialog.Leave(), state.dialog)
        assertTrue(events.isEmpty())
    }

    @Test
    fun `an entry with the most attachments takes no more - none can be added`() = runTest {
        withAttachments(ATTACHMENTS_MAX_PER_ENTRY)
        picked("one more.txt", byteArrayOf(1))

        val (state, _) = editing({ onAttachmentPicked(PICKED) })

        assertFalse(state.canAddAttachment)
        assertEquals(ATTACHMENTS_MAX_PER_ENTRY, state.attachments.size)
        assertTrue(files.journal.isEmpty())
    }

    @Test
    fun `one removed from a full entry lets a file be added again`() = runTest {
        withAttachments(ATTACHMENTS_MAX_PER_ENTRY)

        val (state, _) = editing({ onRemoveAttachment(0) })

        assertTrue(state.canAddAttachment)
    }

    /** The entry [mail] gets [count] attachments. */
    private fun withAttachments(count: Int) {
        val attachments = List(count) { EntryAttachment(id = "hash-$it", name = "file $it.txt", size = 1) }
        attachments.forEach { entries.attachmentBytes[it.id] = byteArrayOf(1) }
        entries.stored[mail] = entries.stored.getValue(mail).copy(attachments = attachments)
    }

    /** The entry [mail] gets the attachment `notes.txt`. */
    private fun withNotes() {
        val notes = EntryAttachment(id = "hash-notes", name = "notes.txt", size = 3)
        entries.attachmentBytes[notes.id] = byteArrayOf(1, 2, 3)
        entries.stored[mail] = entries.stored.getValue(mail).copy(attachments = listOf(notes))
    }

    /** The file the picker hands back is [name], holding [bytes]. */
    private fun picked(name: String, bytes: ByteArray) {
        files.contents[PICKED] = bytes
        files.names[PICKED] = name
    }

    // Groups

    @Test
    fun `an entry opens in its group, every group to choose from with its path, the bin left out`() = runTest {
        // The bin, with a group in it: neither is a place for an entry
        groups.delete(DB, groups.add("old", groups.rootId, "Old"))

        val (state, _) = editing()

        assertEquals(group, state.group)
        assertEquals(
            listOf(GroupChoice(group, listOf("Passwords")), GroupChoice(work, listOf("Passwords", "Work"))),
            state.groups,
        )
        assertFalse(state.changed)
    }

    @Test
    fun `a new entry goes into the group it was made in unless another is chosen`() = runTest {
        val (state, _) = creating()

        assertEquals(group, state.group)
        assertEquals(2, state.groups.size)
    }

    @Test
    fun `another group is a change`() = runTest {
        val (state, _) = editing({ onGroupChosen(work) })

        assertTrue(state.changed)
    }

    @Test
    fun `saving with another group moves the entry there and closes the screen`() = runTest {
        val (_, events) = editing({ onGroupChosen(work) }, { onSave() })

        assertEquals(work, entries.stored.getValue(mail).groupId)
        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }

    @Test
    fun `leaving with another group asks first`() = runTest {
        val (state, events) = editing({ onGroupChosen(work) }, { onBack() })

        assertEquals(EntryEditDialog.Leave(), state.dialog)
        assertTrue(events.isEmpty())
    }

    // Creating

    @Test
    fun `a new entry opens at once with the blank of the preset, nothing changed`() = runTest {
        val (state, _) = creating()

        assertFalse(state.loading)
        assertEquals(StandardPreset.preset.blank(), state.content)
        assertFalse(state.changed)
    }

    @Test
    fun `a filled new entry is created in its group, written, and read in place of the form`() = runTest {
        val (state, events) = creating(*filled(), { onSave() })

        val created = entries.stored.values.last()
        assertEquals(group, created.groupId)
        assertEquals("Bank", created.fields.single { it.key == StandardPreset.TITLE }.value)
        assertEquals(listOf("save"), database.calls)
        val id = entries.stored.keys.last().value
        val read = MviNavEvent.NavigateTo(EntryReadRoute(DB, id), popUpTo = EntryCreateRoute(DB, group.value), inclusive = true)
        assertEquals(listOf(read), events)
        assertTrue(state.problems.isEmpty())
    }

    @Test
    fun `an empty new entry is refused with every required field marked, and nothing is written`() = runTest {
        val (state, events) = creating({ onSave() })

        assertEquals(
            setOf(StandardPreset.TITLE, StandardPreset.USER_NAME, StandardPreset.PASSWORD),
            state.problems.map { (it as FieldProblem.Required).key }.toSet(),
        )
        assertFalse(state.saving)
        assertTrue(database.calls.isEmpty())
        assertTrue(events.isEmpty())
    }

    @Test
    fun `typing into a refused field takes its mark off, the others stay`() = runTest {
        val (state, _) = creating({ onSave() }, { onValueChange(StandardPreset.TITLE, "B") })

        assertFalse(state.isMissing(StandardPreset.TITLE))
        assertTrue(state.isMissing(StandardPreset.USER_NAME))
    }

    @Test
    fun `a new entry not written asks - saving again would make a second one`() = runTest {
        database.saveFailure = CoreException("Io", "disk full")

        val (state, events) = creating(*filled(), { onSave() })

        assertEquals(EntryEditDialog.NotSaved, state.dialog)
        assertFalse(state.saving)
        assertEquals(2, entries.stored.size) // the entry read and the one made
        assertTrue(events.isEmpty())
    }

    @Test
    fun `the not-written dialog closes the screen`() = runTest {
        database.saveFailure = CoreException("Io", "disk full")

        val (_, events) = creating(*filled(), { onSave() }, { onDialogConfirm() })

        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }

    @Test
    fun `dismissing the not-written dialog closes the screen too`() = runTest {
        database.saveFailure = CoreException("Io", "disk full")

        val (_, events) = creating(*filled(), { onSave() }, { onDialogDismiss() })

        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }

    @Test
    fun `a group gone before the save closes the screen`() = runTest {
        entries.failures["create"] = EntryFailure.GroupNotFound()

        val (_, events) = creating(*filled(), { onSave() })

        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }

    @Test
    fun `a save that fails for no known reason stays, says so and goes to the log`() = runTest {
        entries.failures["create"] = EntryFailure.Unexpected(IllegalStateException("broken"))

        val (state, events) = creating(*filled(), { onSave() })

        assertFalse(state.saving)
        assertEquals(EntryEditMessage.Unexpected, state.message)
        assertEquals(1, log.logged.size)
        assertTrue(events.isEmpty())
    }

    // Back

    @Test
    fun `back from an untouched form leaves at once`() = runTest {
        val (state, events) = creating({ onBack() })

        assertNull(state.dialog)
        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }

    @Test
    fun `back from a changed form asks first`() = runTest {
        val (state, events) = creating(typed(), { onBack() })

        assertEquals(EntryEditDialog.Leave(), state.dialog)
        assertTrue(events.isEmpty())
    }

    @Test
    fun `stay closes the question and keeps the form`() = runTest {
        val (state, events) = creating(typed(), { onBack() }, { onDialogDismiss() })

        assertNull(state.dialog)
        assertTrue(state.changed)
        assertTrue(events.isEmpty())
    }

    // Closed - the cross of its tab in a container (pass-docs, Step 98, item 8)

    @Test
    fun `an untouched form of an entry closed goes past its reading`() = runTest {
        val (_, events) = editing({ onClose() })

        assertEquals(listOf(MviNavEvent.NavigateBackTo(EntryReadRoute(DB, mail.value), inclusive = true, orBack = true)), events)
    }

    @Test
    fun `a changed form closed asks first, as back does - the question knows it closes`() = runTest {
        val (state, events) = editing(typed(), { onClose() })

        assertEquals(EntryEditDialog.Leave(closing = true), state.dialog)
        assertTrue(events.isEmpty())
    }

    @Test
    fun `leave on the question of closing goes past the reading`() = runTest {
        val (_, events) = editing(typed(), { onClose() }, { onDialogConfirm() })

        assertEquals(listOf(MviNavEvent.NavigateBackTo(EntryReadRoute(DB, mail.value), inclusive = true, orBack = true)), events)
    }

    @Test
    fun `stay on the question of closing keeps the form, and back after it is back again`() = runTest {
        val (state, events) = editing(typed(), { onClose() }, { onDialogDismiss() }, { onBack() })

        assertEquals(EntryEditDialog.Leave(), state.dialog)
        assertTrue(events.isEmpty())
    }

    @Test
    fun `an untouched new entry closed goes back`() = runTest {
        val (_, events) = creating({ onClose() })

        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }

    @Test
    fun `leave goes without saving`() = runTest {
        val (_, events) = creating(typed(), { onBack() }, { onDialogConfirm() })

        assertEquals(listOf(MviNavEvent.NavigateBack), events)
        assertTrue(database.calls.isEmpty())
    }

    @Test
    fun `a value typed and typed back is no change`() = runTest {
        val (state, events) = editing(
            { onValueChange(StandardPreset.USER_NAME, "you") },
            { onValueChange(StandardPreset.USER_NAME, "me") },
            { onBack() },
        )

        assertFalse(state.changed)
        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }

    // Editing

    @Test
    fun `an entry opens as it is in the database`() = runTest {
        val (state, _) = editing()

        assertFalse(state.loading)
        assertEquals("Mail", state.content?.value(StandardPreset.TITLE))
        assertEquals(listOf("PIN", "Recovery"), state.content?.ownFields?.map { it.key })
        assertFalse(state.changed)
    }

    @Test
    fun `an entry gone before it is read closes the screen`() = runTest {
        val (_, events) = editing(entryId = "gone")

        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }

    @Test
    fun `an entry that cannot be read says so and goes to the log`() = runTest {
        entries.failures["read"] = EntryFailure.Unexpected(IllegalStateException("broken"))

        val (state, _) = editing()

        assertFalse(state.loading)
        assertNull(state.content)
        assertEquals(EntryEditMessage.LoadFailed, state.message)
        assertEquals(1, log.logged.size)
    }

    @Test
    fun `a changed entry is written and the screen closes`() = runTest {
        val (_, events) = editing({ onValueChange(StandardPreset.USER_NAME, "you") }, { onSave() })

        assertEquals("you", entries.stored.getValue(mail).fields.single { it.key == StandardPreset.USER_NAME }.value)
        assertEquals(listOf("save"), database.calls)
        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }

    @Test
    fun `saving an unchanged entry writes nothing and closes`() = runTest {
        val (_, events) = editing({ onSave() })

        assertFalse("update" in entries.calls)
        assertTrue(database.calls.isEmpty())
        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }

    @Test
    fun `a required field emptied is refused`() = runTest {
        val (state, _) = editing({ onValueChange(StandardPreset.PASSWORD, "") }, { onSave() })

        assertEquals(listOf<FieldProblem>(FieldProblem.Required(StandardPreset.PASSWORD)), state.problems)
        assertTrue(database.calls.isEmpty())
    }

    @Test
    fun `the password and protected own fields show`() = runTest {
        val (state, _) = editing({ onTogglePassword() }, { onToggleOwn(0) })

        assertTrue(state.passwordVisible)
        assertEquals(setOf(0), state.shownOwn)
    }

    @Test
    fun `the fields private by the database come with the entry, and the eye shows one`() = runTest {
        settings.entries = settings.entries.copy(protection = FieldProtection.DEFAULT.copy(title = true, url = true))

        val (state, _) = editing({ onToggleKey(StandardPreset.URL) })

        assertEquals(FieldProtection.DEFAULT.copy(title = true, url = true), state.protection)
        assertEquals(setOf(StandardPreset.URL), state.shownKeys)
    }

    @Test
    fun `a new entry is hidden while typed as it will be in the file`() = runTest {
        settings.entries = settings.entries.copy(protection = FieldProtection.DEFAULT.copy(title = true, url = true))

        val (state, _) = creating()

        assertEquals(FieldProtection.DEFAULT.copy(title = true, url = true), state.protection)
    }

    @Test
    fun `the private fields not read - only the password is hidden, the entry opens`() = runTest {
        settings.failure = EntrySettingsFailure.Unexpected()

        val (state, _) = editing()

        assertEquals(FieldProtection.DEFAULT, state.protection)
        assertEquals("Mail", state.content?.value(StandardPreset.TITLE))
        assertEquals(1, log.logged.size)
    }

    @Test
    fun `a shown own field hides again`() = runTest {
        val (state, _) = editing({ onToggleOwn(0) }, { onToggleOwn(0) })

        assertTrue(state.shownOwn.isEmpty())
    }

    @Test
    fun `closing the choice of presets changes nothing`() = runTest {
        val (state, _) = editing({ onChoosePreset() }, { onDialogDismiss() })

        assertNull(state.dialog)
        assertFalse(state.changed)
    }

    @Test
    fun `the preset button opens the choice - presets of one's own, the standard one marked`() = runTest {
        val (state, _) = creating({ onChoosePreset() })

        assertEquals(EntryEditDialog.Preset(listOf(card), selected = null), state.dialog)
    }

    @Test
    fun `a preset chosen lays the form over it - a change, the values by key, the protected pin moves in`() = runTest {
        val (state, _) = editing({ onChoosePreset() }, { onPresetChosen(card.id) })

        val content = state.content!!
        assertNull(state.dialog)
        assertEquals(card, content.preset)
        assertEquals("me", content.value(StandardPreset.USER_NAME))
        assertEquals("1234", content.value("PIN"))
        assertEquals(listOf("Recovery"), content.ownFields.map { it.key })
        assertTrue(state.changed)
    }

    @Test
    fun `a filled field the new preset lacks is hidden, and back on the preset before it is there again`() = runTest {
        val (state, _) = creating(
            { onValueChange(StandardPreset.URL, "bank.ru") },
            { onChoosePreset() },
            { onPresetChosen(card.id) },
            { onChoosePreset() },
            { onPresetChosen(null) },
        )

        assertEquals(StandardPreset.preset, state.content?.preset)
        assertEquals("bank.ru", state.content?.value(StandardPreset.URL))
        assertEquals(emptyList<EntryField>(), state.hidden)
    }

    @Test
    fun `while a preset of one's own is chosen, what the standard one had and it lacks is hidden`() = runTest {
        val (state, _) = creating(
            { onValueChange(StandardPreset.URL, "bank.ru") },
            { onChoosePreset() },
            { onPresetChosen(card.id) },
        )

        assertEquals(listOf(field(StandardPreset.URL, "bank.ru")), state.hidden)
    }

    @Test
    fun `create preset leaves for its screen, remembering the presets there were`() = runTest {
        val (state, events) = creating({ onChoosePreset() }, { onCreatePreset() })

        assertNull(state.dialog)
        assertEquals(setOf(checkNotNull(card.id)), state.presetsBeforeCreate)
        assertEquals(listOf(MviNavEvent.NavigateTo(PresetEditRoute())), events)
    }

    @Test
    fun `back from the screen of presets, the one made there is picked`() = runTest {
        val wifi = CustomPreset.of(PresetId("wifi"), "Wi-Fi", listOf(listOf("SSID")))
        presets.save(wifi)
        val blank = StandardPreset.preset.blank()

        val (state, _) = editing(
            { onResumed() },
            initial = EntryEditState(creating = true, content = blank, initial = blank, group = group, presetsBeforeCreate = setOf(card.id!!)),
        )

        assertEquals(wifi, state.content!!.preset)
        assertNull(state.presetsBeforeCreate)
    }

    @Test
    fun `back from the screen of presets with none made, the form stays as it was`() = runTest {
        val blank = StandardPreset.preset.blank()

        val (state, _) = editing(
            { onResumed() },
            initial = EntryEditState(creating = true, content = blank, initial = blank, group = group, presetsBeforeCreate = setOf(card.id!!)),
        )

        assertEquals(StandardPreset.preset, state.content!!.preset)
        assertNull(state.presetsBeforeCreate)
    }

    @Test
    fun `shown again with nothing asked for, the form stays as it was`() = runTest {
        presets.save(CustomPreset.of(PresetId("wifi"), "Wi-Fi", listOf(listOf("SSID"))))

        val (state, _) = editing({ onResumed() })

        assertEquals(StandardPreset.preset, state.content!!.preset)
    }

    @Test
    fun `saving with hidden fields asks first, naming them, and writes nothing`() = runTest {
        val (state, _) = editing(
            { onValueChange(StandardPreset.URL, "mail.ru") },
            { onChoosePreset() },
            { onPresetChosen(card.id) },
            { onSave() },
        )

        assertEquals(EntryEditDialog.HiddenFields(listOf(StandardPreset.URL)), state.dialog)
        assertFalse(state.saving)
        assertEquals("", entries.stored.getValue(mail).fields.first { it.key == StandardPreset.URL }.value)
    }

    @Test
    fun `save as is writes the entry of the new preset without the hidden fields`() = runTest {
        val (_, events) = editing(
            { onValueChange(StandardPreset.URL, "mail.ru") },
            { onChoosePreset() },
            { onPresetChosen(card.id) },
            { onSave() },
            { onDialogConfirm() },
        )

        val written = entries.stored.getValue(mail).fields
        assertFalse(written.any { it.key == StandardPreset.URL })
        assertEquals(field(PresetLink.KEY, "card"), written.last())
        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }

    @Test
    fun `add outside the preset writes the hidden fields as own ones`() = runTest {
        val (_, events) = editing(
            { onValueChange(StandardPreset.URL, "mail.ru") },
            { onChoosePreset() },
            { onPresetChosen(card.id) },
            { onSave() },
            { onKeepHidden() },
        )

        val written = entries.stored.getValue(mail).fields
        assertEquals("mail.ru", written.first { it.key == StandardPreset.URL }.value)
        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }

    @Test
    fun `cancel stays with the hidden fields still hidden`() = runTest {
        val (state, events) = editing(
            { onValueChange(StandardPreset.URL, "mail.ru") },
            { onChoosePreset() },
            { onPresetChosen(card.id) },
            { onSave() },
            { onDialogDismiss() },
        )

        assertNull(state.dialog)
        assertEquals(listOf(field(StandardPreset.URL, "mail.ru")), state.hidden)
        assertEquals(emptyList<MviNavEvent>(), events)
    }

    @Test
    fun `choosing a preset forgets what was shown in clear and what was refused`() = runTest {
        val (state, _) = creating(
            { onSave() },
            { onChoosePreset() },
            { onPresetChosen(card.id) },
        )

        assertEquals(emptyList<FieldProblem>(), state.problems)
    }

    // Own fields

    @Test
    fun `an added field goes last, without the spaces around its name, empty and not protected`() = runTest {
        val (state, _) = editing(
            { onAddField() },
            { onDialogNameChange(" Answer ") },
            { onDialogConfirm() },
        )

        assertNull(state.dialog)
        assertEquals(field("Answer", ""), state.content?.ownFields?.last())
        assertTrue(state.changed)
    }

    @Test
    fun `an added field named as a field of the preset stays in the dialog with the reason`() = runTest {
        val (state, _) = adding("password")

        assertEquals(EntryEditDialog.AddField("password", problem = NameProblem.TAKEN), state.dialog)
    }

    @Test
    fun `an added field named as another own one, in another case, stays in the dialog`() = runTest {
        val (state, _) = adding("pin")

        assertEquals(EntryEditDialog.AddField("pin", problem = NameProblem.TAKEN), state.dialog)
        assertEquals(2, state.content?.ownFields?.size)
    }

    @Test
    fun `an added field with no name stays in the dialog`() = runTest {
        val (state, _) = adding("  ")

        assertEquals(EntryEditDialog.AddField("  ", problem = NameProblem.EMPTY), state.dialog)
    }

    @Test
    fun `a renamed field keeps its value`() = runTest {
        val (state, _) = editing(
            { onEditField(1) },
            { onDialogNameChange("Recovery words") },
            { onDialogConfirm() },
        )

        assertEquals(field("Recovery words", "words"), state.content?.ownFields?.get(1))
    }

    @Test
    fun `a field renamed to its own name is no rival and no change`() = runTest {
        val (state, _) = editing({ onEditField(1) }, { onDialogConfirm() })

        assertNull(state.dialog)
        assertFalse(state.changed)
    }

    @Test
    fun `edit starts from the current name and protection`() = runTest {
        val (state, _) = editing({ onEditField(0) })

        assertEquals(EntryEditDialog.EditField(0, "PIN", protected = true, type = null), state.dialog)
    }

    @Test
    fun `an added field is not protected unless switched`() = runTest {
        val (state, _) = adding("Code")

        assertEquals(field("Code", ""), state.content?.ownFields?.last())
    }

    @Test
    fun `an added field switched to protected is protected`() = runTest {
        val (state, _) = editing(
            { onAddField() },
            { onDialogNameChange("Code") },
            { onDialogProtectedChange(true) },
            { onDialogConfirm() },
        )

        assertEquals(field("Code", "", protected = true), state.content?.ownFields?.last())
    }

    @Test
    fun `an edited field takes the protection switched, keeps its value, and is a change`() = runTest {
        val (state, _) = editing(
            { onEditField(0) },
            { onDialogProtectedChange(false) },
            { onDialogConfirm() },
        )

        assertEquals(field("PIN", "1234"), state.content?.ownFields?.get(0))
        assertTrue(state.changed)
    }

    @Test
    fun `a field made protected starts hidden`() = runTest {
        val (state, _) = editing(
            { onToggleOwn(1) },
            { onEditField(1) },
            { onDialogProtectedChange(true) },
            { onDialogConfirm() },
        )

        assertEquals(field("Recovery", "words", protected = true), state.content?.ownFields?.get(1))
        assertTrue(state.shownOwn.isEmpty())
    }

    @Test
    fun `the protection switched is saved into the entry`() = runTest {
        editing(
            { onEditField(1) },
            { onDialogProtectedChange(true) },
            { onDialogConfirm() },
            { onSave() },
        )

        assertEquals(listOf(true), entries.stored.getValue(mail).fields.filter { it.key == "Recovery" }.map { it.protected })
    }

    // The type of a field (pass-docs, Step 75, item 3) - "card" is one this app does not know: a newer client wrote it

    @Test
    fun `edit starts from the type of the field as the file gives it`() = runTest {
        val (state, _) = editing({ onEditField(0) }, entryId = cardEntry())

        assertEquals(EntryEditDialog.EditField(0, "Card", protected = false, type = "card"), state.dialog)
    }

    @Test
    fun `a type this app does not know stays, with the value, until another is picked`() = runTest {
        val (state, _) = editing(
            { onEditField(0) },
            { onDialogNameChange("Card number") },
            { onDialogConfirm() },
            entryId = cardEntry(),
        )

        assertEquals(field("Card number", "4111", type = "card"), state.content?.ownFields?.get(0))
    }

    @Test
    fun `another type picked clears the value of the field and is a change`() = runTest {
        val (state, _) = editing(
            { onEditField(0) },
            { onDialogTypeChange(FieldType.TEXT) },
            { onDialogConfirm() },
            entryId = cardEntry(),
        )

        assertEquals(field("Card", ""), state.content?.ownFields?.get(0))
        assertTrue(state.changed)
    }

    @Test
    fun `a type this app does not know is saved into the entry as it came`() = runTest {
        val id = cardEntry()
        editing({ onValueChange(StandardPreset.TITLE, "Bank card") }, { onSave() }, entryId = id)

        assertEquals(listOf("card"), entries.stored.getValue(EntryId(id)).fields.filter { it.key == "Card" }.map { it.type })
    }

    @Test
    fun `an added field is text unless another type is picked`() = runTest {
        val (state, _) = editing(
            { onAddField() },
            { onDialogNameChange("Code") },
            { onDialogTypeChange(FieldType.TEXT) },
            { onDialogConfirm() },
        )

        assertNull(state.content?.ownFields?.last()?.type)
    }

    // A phone field (pass-docs, Step 77) - the country by default here is RU

    @Test
    fun `a phone field added takes the country by default`() = runTest {
        val (state, _) = editing(
            { onAddField() },
            { onDialogNameChange("Phone") },
            { onDialogTypeChange(FieldType.PHONE) },
            { onDialogConfirm() },
        )

        assertEquals(field("Phone", "", type = "phone:RU"), state.content?.ownFields?.last())
    }

    @Test
    fun `the countries of the phone fields and the one by default are read ahead`() = runTest {
        editing(entryId = phoneEntry())

        assertEquals(setOf("KZ", "RU"), phones.prepared.toSet())
    }

    @Test
    fun `digits typed are kept in E164 of the country of the field`() = runTest {
        val (state, _) = editing({ onPhoneDigitsChange(PhoneTarget.Own(0), "7011112233") }, entryId = phoneEntry())

        assertEquals(field("Phone", "+77011112233", type = "phone:KZ"), state.content?.ownFields?.get(0))
        assertTrue(state.changed)
    }

    @Test
    fun `the prefix asks for the country, the one of the field chosen`() = runTest {
        val (state, _) = editing({ onChoosePhoneCountry(PhoneTarget.Own(0)) }, entryId = phoneEntry())

        val dialog = state.dialog as EntryEditDialog.PhoneCountries
        assertEquals("KZ", dialog.selected)
        assertEquals(PhoneCountry("NL", 31), dialog.countries.last())
    }

    @Test
    fun `another country keeps the digits of the number, the calling code changed`() = runTest {
        val (state, _) = editing(
            { onChoosePhoneCountry(PhoneTarget.Own(0)) },
            { onPhoneCountryChosen("NL") },
            entryId = phoneEntry(),
        )

        assertEquals(field("Phone", "+317011234567", type = "phone:NL"), state.content?.ownFields?.get(0))
        assertNull(state.dialog)
        assertTrue("NL" in phones.prepared)
    }

    @Test
    fun `a phone field made text again loses its number`() = runTest {
        val (state, _) = editing(
            { onEditField(0) },
            { onDialogTypeChange(FieldType.TEXT) },
            { onDialogConfirm() },
            entryId = phoneEntry(),
        )

        assertEquals(field("Phone", ""), state.content?.ownFields?.get(0))
    }

    @Test
    fun `phone picked again keeps the country and the number`() = runTest {
        val (state, _) = editing(
            { onEditField(0) },
            { onDialogTypeChange(FieldType.PHONE) },
            { onDialogConfirm() },
            entryId = phoneEntry(),
        )

        assertEquals(field("Phone", "+77011234567", type = "phone:KZ"), state.content?.ownFields?.get(0))
        assertFalse(state.changed)
    }

    @Test
    fun `delete asks first, naming the field`() = runTest {
        val (state, _) = editing({ onDeleteField(0) })

        assertEquals(EntryEditDialog.DeleteField(0, "PIN"), state.dialog)
    }

    @Test
    fun `a deleted field leaves the form, and what is shown of the fields after it moves up`() = runTest {
        val (state, _) = editing(
            { onAddField() },
            { onDialogNameChange("Code") },
            { onDialogConfirm() },
            { onToggleOwn(2) },
            { onDeleteField(0) },
            { onDialogConfirm() },
        )

        assertEquals(listOf("Recovery", "Code"), state.content?.ownFields?.map { it.key })
        assertEquals(setOf(1), state.shownOwn)
    }

    @Test
    fun `a full card of own fields takes no more`() = runTest {
        val (state, _) = editing(*fillOwnCard(), { onAddField() })

        assertEquals(12, state.content?.ownFields?.size)
        assertFalse(state.canAddField)
        assertNull(state.dialog)
    }

    @Test
    fun `a field deleted from a full card makes room again`() = runTest {
        val (state, _) = editing(*fillOwnCard(), { onDeleteField(0) }, { onDialogConfirm() })

        assertEquals(11, state.content?.ownFields?.size)
        assertTrue(state.canAddField)
    }

    @Test
    fun `a twin of another program is refused at save once anything changes`() = runTest {
        val twin = twin()

        val (state, events) = editing(typed(StandardPreset.USER_NAME), { onSave() }, entryId = twin)

        assertEquals(listOf<FieldProblem>(FieldProblem.BadName(1, NameProblem.TAKEN)), state.problems)
        assertEquals(NameProblem.TAKEN, state.nameProblem(1))
        assertTrue(events.isEmpty())
    }

    @Test
    fun `deleting the twin lets the entry go`() = runTest {
        val twin = twin()

        val (state, events) = editing(
            typed(StandardPreset.USER_NAME),
            { onSave() },
            { onDeleteField(1) },
            { onDialogConfirm() },
            { onSave() },
            entryId = twin,
        )

        assertTrue(state.problems.isEmpty())
        assertEquals(listOf("1"), entries.stored.getValue(EntryId(twin)).fields.filter { it.key == "PIN" }.map { it.value })
        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }

    @Test
    fun `nothing changes while the save is under way`() = runTest {
        val (state, _) = editing(
            { onValueChange(StandardPreset.TITLE, "Changed") },
            { onAddField() },
            initial = opener.open(DB, mail).content.let { EntryEditState(creating = false, content = it, initial = it, saving = true) },
        )

        assertEquals("Mail", state.content?.value(StandardPreset.TITLE))
        assertNull(state.dialog)
    }

    // Delete, from the menu of the top bar

    @Test
    fun `delete asks first, naming the entry as the file holds it`() = runTest {
        val (state, _) = editing({ onValueChange(StandardPreset.TITLE, "Typed") }, { onDelete() })

        assertEquals(EntryEditDialog.Delete("Mail", toBin = true), state.dialog)
    }

    @Test
    fun `without a recycle bin the dialog says the delete is for good`() = runTest {
        trash.enabled = false

        val (state, _) = editing({ onDelete() })

        assertEquals(EntryEditDialog.Delete("Mail", toBin = false), state.dialog)
    }

    @Test
    fun `a new entry has nothing to delete`() = runTest {
        val (state, _) = creating({ onDelete() })

        assertNull(state.dialog)
    }

    @Test
    fun `a deleted entry goes to the recycle bin with what was typed, unasked, and the screen goes to the list`() = runTest {
        val (_, events) = editing(
            { onValueChange(StandardPreset.TITLE, "Typed") },
            { onDelete() },
            { onDialogConfirm() },
        )

        assertEquals(listOf(mail), entries.recycled)
        assertEquals(listOf("save"), database.calls)
        // Past the reading of the entry, if the form was opened from it; plain "back" otherwise
        assertEquals(
            listOf(MviNavEvent.NavigateBackTo(EntryReadRoute(DB, mail.value), inclusive = true, orBack = true)),
            events,
        )
    }

    @Test
    fun `a deleted entry that is not written stays on the form, and the snackbar says so`() = runTest {
        database.saveFailure = CoreException("Io", "disk full")

        val (state, events) = editing({ onDelete() }, { onDialogConfirm() })

        assertNull(state.dialog)
        assertEquals(EntryEditMessage.NotSaved, state.message)
        assertTrue(events.isEmpty())
    }

    @Test
    fun `a delete under way does not close`() = runTest {
        val read = opener.open(DB, mail).content
        val (state, _) = editing(
            { onDialogDismiss() },
            initial = EntryEditState(
                creating = false,
                content = read,
                initial = read,
                dialog = EntryEditDialog.Delete("Mail", toBin = true, deleting = true),
            ),
        )

        assertEquals(EntryEditDialog.Delete("Mail", toBin = true, deleting = true), state.dialog)
    }

    // Helpers

    private fun typed(key: String = StandardPreset.TITLE): EntryEditActions.() -> Unit = { onValueChange(key, "typed") }

    private suspend fun TestScope.adding(name: String) =
        editing({ onAddField() }, { onDialogNameChange(name) }, { onDialogConfirm() })

    /** An entry with an own phone field of Kazakhstan; its id. */
    private fun phoneEntry(): String = entries.add(
        "phone",
        group,
        listOf(
            field(StandardPreset.TITLE, "Bank"),
            field(StandardPreset.USER_NAME, "me"),
            field(StandardPreset.PASSWORD, "secret", protected = true),
            field("Phone", "+77011234567", type = "phone:KZ"),
        ),
    ).value

    /** An entry with an own field of a type this app does not know; its id. */
    private fun cardEntry(): String = entries.add(
        "typed",
        group,
        listOf(
            field(StandardPreset.TITLE, "Bank"),
            field(StandardPreset.USER_NAME, "me"),
            field(StandardPreset.PASSWORD, "secret", protected = true),
            field("Card", "4111", type = "card"),
        ),
    ).value

    /** An entry of another program with two fields under one key; its id. */
    private fun twin(): String = entries.add(
        "twin",
        group,
        listOf(
            field(StandardPreset.TITLE, "Twin"),
            field(StandardPreset.USER_NAME, "me"),
            field(StandardPreset.PASSWORD, "secret"),
            field("PIN", "1"),
            field("PIN", "2"),
        ),
    ).value

    /** The entry holds two own fields; ten more fill the card. */
    private fun fillOwnCard() = (1..10).flatMap {
        listOf<EntryEditActions.() -> Unit>({ onAddField() }, { onDialogNameChange("F$it") }, { onDialogConfirm() })
    }.toTypedArray()

    private fun filled() = arrayOf<EntryEditActions.() -> Unit>(
        { onValueChange(StandardPreset.TITLE, "Bank") },
        { onValueChange(StandardPreset.USER_NAME, "me") },
        { onValueChange(StandardPreset.PASSWORD, "secret") },
    )

    private suspend fun TestScope.creating(vararg actions: EntryEditActions.() -> Unit) =
        perform(groupId = group.value, entryId = null, actions = actions)

    private suspend fun TestScope.editing(
        vararg actions: EntryEditActions.() -> Unit,
        entryId: String = mail.value,
        initial: EntryEditState? = null,
    ) = perform(groupId = null, entryId = entryId, actions = actions, initial = initial)

    /**
     * Runs [actions] one after another on a new processor, after it has read its entry - or on [initial],
     * as given; the state they end in, and the navigation.
     */
    private suspend fun TestScope.perform(
        groupId: String?,
        entryId: String?,
        actions: Array<out EntryEditActions.() -> Unit>,
        initial: EntryEditState? = null,
    ): Pair<EntryEditState, List<MviNavEvent>> {
        val (navigator, events) = recordNavigation()
        val processor = EntryEditProcessor(
            DB,
            groupId,
            entryId,
            opener,
            settings,
            groups,
            EntryEditor(entries, opener, settings, database),
            trash,
            presets,
            phones,
            phoneDefaults,
            AttachmentFiles(files, FakeAttachmentRepository(entries)),
            generator,
            navigator,
            log,
        )

        processor.testWithInternalState(this, initial) {
            if (initial == null) runOnCreate().join()
            containerHost.runEach(actions)
            cancelAndIgnoreRemainingItems()
        }
        return processor.container.stateFlow.value to events
    }

    private fun field(key: String, value: String, protected: Boolean = false, type: String? = null) =
        EntryField(key, value, protected, type)

    private companion object {
        const val PICKED = "content://picked"
    }
}
