package ru.kino.dev.vault.pres.entryedit

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.orbitmvi.orbit.test.testWithInternalState
import ru.kino.dev.core.CoreException
import ru.kino.dev.core.preset.CustomPreset
import ru.kino.dev.core.preset.FakePresetRepository
import ru.kino.dev.core.preset.PresetId
import ru.kino.dev.core.preset.PresetLink
import ru.kino.dev.core.preset.StandardPreset
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.vault.FakeVaultDatabase
import ru.kino.dev.vault.entry.EntryEditor
import ru.kino.dev.vault.entry.EntryFailure
import ru.kino.dev.vault.entry.EntryField
import ru.kino.dev.vault.entry.EntryOpener
import ru.kino.dev.vault.entry.FakeEntryRepository
import ru.kino.dev.vault.entry.FieldProblem
import ru.kino.dev.vault.entry.NameProblem
import ru.kino.dev.vault.entry.blank
import ru.kino.dev.vault.group.EntryId
import ru.kino.dev.vault.group.FakeGroupRepository
import ru.kino.dev.vault.group.GroupId
import ru.kino.dev.vault.pres.common.GroupChoice
import ru.kino.dev.vault.pres.navigation.PresetCreateRoute
import ru.kino.dev.vault.pres.testing.DB
import ru.kino.dev.vault.pres.testing.RecordingLog
import ru.kino.dev.vault.pres.testing.recordNavigation

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

    // Groups

    @Test
    fun `an entry opens in its group, every group to choose from with its path, the bin left out`() = runTest {
        // The bin, with a group in it: neither is a place for an entry
        groups.moveToRecycleBin(DB, groups.add("old", groups.rootId, "Old"))

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
        val (state, _) = editing(EntryEditAction.GroupChosen(work))

        assertTrue(state.changed)
    }

    @Test
    fun `saving with another group moves the entry there and closes the screen`() = runTest {
        val (_, events) = editing(EntryEditAction.GroupChosen(work), EntryEditAction.Save)

        assertEquals(work, entries.stored.getValue(mail).groupId)
        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }

    @Test
    fun `leaving with another group asks first`() = runTest {
        val (state, events) = editing(EntryEditAction.GroupChosen(work), EntryEditAction.Back)

        assertEquals(EntryEditDialog.Leave, state.dialog)
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
    fun `a filled new entry is created in its group, written, and the screen closes`() = runTest {
        val (state, events) = creating(*filled(), EntryEditAction.Save)

        val created = entries.stored.values.last()
        assertEquals(group, created.groupId)
        assertEquals("Bank", created.fields.single { it.key == StandardPreset.TITLE }.value)
        assertEquals(listOf("save"), database.calls)
        assertEquals(listOf(MviNavEvent.NavigateBack), events)
        assertTrue(state.problems.isEmpty())
    }

    @Test
    fun `an empty new entry is refused with every required field marked, and nothing is written`() = runTest {
        val (state, events) = creating(EntryEditAction.Save)

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
        val (state, _) = creating(EntryEditAction.Save, EntryEditAction.ValueChanged(StandardPreset.TITLE, "B"))

        assertFalse(state.isMissing(StandardPreset.TITLE))
        assertTrue(state.isMissing(StandardPreset.USER_NAME))
    }

    @Test
    fun `a new entry not written asks - saving again would make a second one`() = runTest {
        database.saveFailure = CoreException("Io", "disk full")

        val (state, events) = creating(*filled(), EntryEditAction.Save)

        assertEquals(EntryEditDialog.NotSaved, state.dialog)
        assertFalse(state.saving)
        assertEquals(2, entries.stored.size) // the entry read and the one made
        assertTrue(events.isEmpty())
    }

    @Test
    fun `the not-written dialog closes the screen`() = runTest {
        database.saveFailure = CoreException("Io", "disk full")

        val (_, events) = creating(*filled(), EntryEditAction.Save, EntryEditAction.DialogConfirm)

        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }

    @Test
    fun `dismissing the not-written dialog closes the screen too`() = runTest {
        database.saveFailure = CoreException("Io", "disk full")

        val (_, events) = creating(*filled(), EntryEditAction.Save, EntryEditAction.DialogDismiss)

        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }

    @Test
    fun `a group gone before the save closes the screen`() = runTest {
        entries.failures["create"] = EntryFailure.GroupNotFound()

        val (_, events) = creating(*filled(), EntryEditAction.Save)

        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }

    @Test
    fun `a save that fails for no known reason stays, says so and goes to the log`() = runTest {
        entries.failures["create"] = EntryFailure.Unexpected(IllegalStateException("broken"))

        val (state, events) = creating(*filled(), EntryEditAction.Save)

        assertFalse(state.saving)
        assertEquals(EntryEditMessage.Unexpected, state.message)
        assertEquals(1, log.logged.size)
        assertTrue(events.isEmpty())
    }

    // Back

    @Test
    fun `back from an untouched form leaves at once`() = runTest {
        val (state, events) = creating(EntryEditAction.Back)

        assertNull(state.dialog)
        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }

    @Test
    fun `back from a changed form asks first`() = runTest {
        val (state, events) = creating(typed(), EntryEditAction.Back)

        assertEquals(EntryEditDialog.Leave, state.dialog)
        assertTrue(events.isEmpty())
    }

    @Test
    fun `stay closes the question and keeps the form`() = runTest {
        val (state, events) = creating(typed(), EntryEditAction.Back, EntryEditAction.DialogDismiss)

        assertNull(state.dialog)
        assertTrue(state.changed)
        assertTrue(events.isEmpty())
    }

    @Test
    fun `leave goes without saving`() = runTest {
        val (_, events) = creating(typed(), EntryEditAction.Back, EntryEditAction.DialogConfirm)

        assertEquals(listOf(MviNavEvent.NavigateBack), events)
        assertTrue(database.calls.isEmpty())
    }

    @Test
    fun `a value typed and typed back is no change`() = runTest {
        val (state, events) = editing(
            EntryEditAction.ValueChanged(StandardPreset.USER_NAME, "you"),
            EntryEditAction.ValueChanged(StandardPreset.USER_NAME, "me"),
            EntryEditAction.Back,
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
        val (_, events) = editing(EntryEditAction.ValueChanged(StandardPreset.USER_NAME, "you"), EntryEditAction.Save)

        assertEquals("you", entries.stored.getValue(mail).fields.single { it.key == StandardPreset.USER_NAME }.value)
        assertEquals(listOf("save"), database.calls)
        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }

    @Test
    fun `saving an unchanged entry writes nothing and closes`() = runTest {
        val (_, events) = editing(EntryEditAction.Save)

        assertFalse("update" in entries.calls)
        assertTrue(database.calls.isEmpty())
        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }

    @Test
    fun `a required field emptied is refused`() = runTest {
        val (state, _) = editing(EntryEditAction.ValueChanged(StandardPreset.PASSWORD, ""), EntryEditAction.Save)

        assertEquals(listOf<FieldProblem>(FieldProblem.Required(StandardPreset.PASSWORD)), state.problems)
        assertTrue(database.calls.isEmpty())
    }

    @Test
    fun `the password and protected own fields show`() = runTest {
        val (state, _) = editing(EntryEditAction.TogglePassword, EntryEditAction.ToggleOwn(0))

        assertTrue(state.passwordVisible)
        assertEquals(setOf(0), state.shownOwn)
    }

    @Test
    fun `a shown own field hides again`() = runTest {
        val (state, _) = editing(EntryEditAction.ToggleOwn(0), EntryEditAction.ToggleOwn(0))

        assertTrue(state.shownOwn.isEmpty())
    }

    @Test
    fun `closing the choice of presets changes nothing`() = runTest {
        val (state, _) = editing(EntryEditAction.ChoosePreset, EntryEditAction.DialogDismiss)

        assertNull(state.dialog)
        assertFalse(state.changed)
    }

    @Test
    fun `the preset button opens the choice - presets of one's own, the standard one marked`() = runTest {
        val (state, _) = creating(EntryEditAction.ChoosePreset)

        assertEquals(EntryEditDialog.Preset(listOf(card), selected = null), state.dialog)
    }

    @Test
    fun `a preset chosen lays the form over it - a change, the values by key, the protected pin moves in`() = runTest {
        val (state, _) = editing(EntryEditAction.ChoosePreset, EntryEditAction.PresetChosen(card.id))

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
            EntryEditAction.ValueChanged(StandardPreset.URL, "bank.ru"),
            EntryEditAction.ChoosePreset,
            EntryEditAction.PresetChosen(card.id),
            EntryEditAction.ChoosePreset,
            EntryEditAction.PresetChosen(null),
        )

        assertEquals(StandardPreset.preset, state.content!!.preset)
        assertEquals("bank.ru", state.content!!.value(StandardPreset.URL))
        assertEquals(emptyList<EntryField>(), state.hidden)
    }

    @Test
    fun `while a preset of one's own is chosen, what the standard one had and it lacks is hidden`() = runTest {
        val (state, _) = creating(
            EntryEditAction.ValueChanged(StandardPreset.URL, "bank.ru"),
            EntryEditAction.ChoosePreset,
            EntryEditAction.PresetChosen(card.id),
        )

        assertEquals(listOf(field(StandardPreset.URL, "bank.ru")), state.hidden)
    }

    @Test
    fun `create preset leaves for its screen, remembering the presets there were`() = runTest {
        val (state, events) = creating(EntryEditAction.ChoosePreset, EntryEditAction.CreatePreset)

        assertNull(state.dialog)
        assertEquals(setOf(card.id), state.presetsBeforeCreate)
        assertEquals(listOf(MviNavEvent.NavigateTo(PresetCreateRoute)), events)
    }

    @Test
    fun `back from the screen of presets, the one made there is picked`() = runTest {
        val wifi = CustomPreset.of(PresetId("wifi"), "Wi-Fi", listOf(listOf("SSID")))
        presets.save(wifi)
        val blank = StandardPreset.preset.blank()

        val (state, _) = editing(
            EntryEditAction.Resumed,
            initial = EntryEditState(creating = true, content = blank, initial = blank, group = group, presetsBeforeCreate = setOf(card.id!!)),
        )

        assertEquals(wifi, state.content!!.preset)
        assertNull(state.presetsBeforeCreate)
    }

    @Test
    fun `back from the screen of presets with none made, the form stays as it was`() = runTest {
        val blank = StandardPreset.preset.blank()

        val (state, _) = editing(
            EntryEditAction.Resumed,
            initial = EntryEditState(creating = true, content = blank, initial = blank, group = group, presetsBeforeCreate = setOf(card.id!!)),
        )

        assertEquals(StandardPreset.preset, state.content!!.preset)
        assertNull(state.presetsBeforeCreate)
    }

    @Test
    fun `shown again with nothing asked for, the form stays as it was`() = runTest {
        presets.save(CustomPreset.of(PresetId("wifi"), "Wi-Fi", listOf(listOf("SSID"))))

        val (state, _) = editing(EntryEditAction.Resumed)

        assertEquals(StandardPreset.preset, state.content!!.preset)
    }

    @Test
    fun `saving with hidden fields asks first, naming them, and writes nothing`() = runTest {
        val (state, _) = editing(
            EntryEditAction.ValueChanged(StandardPreset.URL, "mail.ru"),
            EntryEditAction.ChoosePreset,
            EntryEditAction.PresetChosen(card.id),
            EntryEditAction.Save,
        )

        assertEquals(EntryEditDialog.HiddenFields(listOf(StandardPreset.URL)), state.dialog)
        assertFalse(state.saving)
        assertEquals("", entries.stored.getValue(mail).fields.first { it.key == StandardPreset.URL }.value)
    }

    @Test
    fun `save as is writes the entry of the new preset without the hidden fields`() = runTest {
        val (_, events) = editing(
            EntryEditAction.ValueChanged(StandardPreset.URL, "mail.ru"),
            EntryEditAction.ChoosePreset,
            EntryEditAction.PresetChosen(card.id),
            EntryEditAction.Save,
            EntryEditAction.DialogConfirm,
        )

        val written = entries.stored.getValue(mail).fields
        assertFalse(written.any { it.key == StandardPreset.URL })
        assertEquals(field(PresetLink.KEY, "card"), written.last())
        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }

    @Test
    fun `add outside the preset writes the hidden fields as own ones`() = runTest {
        val (_, events) = editing(
            EntryEditAction.ValueChanged(StandardPreset.URL, "mail.ru"),
            EntryEditAction.ChoosePreset,
            EntryEditAction.PresetChosen(card.id),
            EntryEditAction.Save,
            EntryEditAction.KeepHidden,
        )

        val written = entries.stored.getValue(mail).fields
        assertEquals("mail.ru", written.first { it.key == StandardPreset.URL }.value)
        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }

    @Test
    fun `cancel stays with the hidden fields still hidden`() = runTest {
        val (state, events) = editing(
            EntryEditAction.ValueChanged(StandardPreset.URL, "mail.ru"),
            EntryEditAction.ChoosePreset,
            EntryEditAction.PresetChosen(card.id),
            EntryEditAction.Save,
            EntryEditAction.DialogDismiss,
        )

        assertNull(state.dialog)
        assertEquals(listOf(field(StandardPreset.URL, "mail.ru")), state.hidden)
        assertEquals(emptyList<MviNavEvent>(), events)
    }

    @Test
    fun `choosing a preset forgets what was shown in clear and what was refused`() = runTest {
        val (state, _) = creating(
            EntryEditAction.Save,
            EntryEditAction.ChoosePreset,
            EntryEditAction.PresetChosen(card.id),
        )

        assertEquals(emptyList<FieldProblem>(), state.problems)
    }

    // Own fields

    @Test
    fun `an added field goes last, without the spaces around its name, empty and not protected`() = runTest {
        val (state, _) = editing(
            EntryEditAction.AddField,
            EntryEditAction.DialogNameChanged(" Answer "),
            EntryEditAction.DialogConfirm,
        )

        assertNull(state.dialog)
        assertEquals(field("Answer", ""), state.content?.ownFields?.last())
        assertTrue(state.changed)
    }

    @Test
    fun `an added field named as a field of the preset stays in the dialog with the reason`() = runTest {
        val (state, _) = adding("password")

        assertEquals(EntryEditDialog.AddField("password", NameProblem.TAKEN), state.dialog)
    }

    @Test
    fun `an added field named as another own one, in another case, stays in the dialog`() = runTest {
        val (state, _) = adding("pin")

        assertEquals(EntryEditDialog.AddField("pin", NameProblem.TAKEN), state.dialog)
        assertEquals(2, state.content?.ownFields?.size)
    }

    @Test
    fun `an added field with no name stays in the dialog`() = runTest {
        val (state, _) = adding("  ")

        assertEquals(EntryEditDialog.AddField("  ", NameProblem.EMPTY), state.dialog)
    }

    @Test
    fun `a renamed field keeps its value`() = runTest {
        val (state, _) = editing(
            EntryEditAction.RenameField(1),
            EntryEditAction.DialogNameChanged("Recovery words"),
            EntryEditAction.DialogConfirm,
        )

        assertEquals(field("Recovery words", "words"), state.content?.ownFields?.get(1))
    }

    @Test
    fun `a field renamed to its own name is no rival and no change`() = runTest {
        val (state, _) = editing(EntryEditAction.RenameField(1), EntryEditAction.DialogConfirm)

        assertNull(state.dialog)
        assertFalse(state.changed)
    }

    @Test
    fun `rename starts from the current name`() = runTest {
        val (state, _) = editing(EntryEditAction.RenameField(0))

        assertEquals(EntryEditDialog.RenameField(0, "PIN"), state.dialog)
    }

    @Test
    fun `delete asks first, naming the field`() = runTest {
        val (state, _) = editing(EntryEditAction.DeleteField(0))

        assertEquals(EntryEditDialog.DeleteField(0, "PIN"), state.dialog)
    }

    @Test
    fun `a deleted field leaves the form, and what is shown of the fields after it moves up`() = runTest {
        val (state, _) = editing(
            EntryEditAction.AddField,
            EntryEditAction.DialogNameChanged("Code"),
            EntryEditAction.DialogConfirm,
            EntryEditAction.ToggleOwn(2),
            EntryEditAction.DeleteField(0),
            EntryEditAction.DialogConfirm,
        )

        assertEquals(listOf("Recovery", "Code"), state.content?.ownFields?.map { it.key })
        assertEquals(setOf(1), state.shownOwn)
    }

    @Test
    fun `a twin of another program is refused at save once anything changes`() = runTest {
        val twin = twin()

        val (state, events) = editing(typed(StandardPreset.USER_NAME), EntryEditAction.Save, entryId = twin)

        assertEquals(listOf<FieldProblem>(FieldProblem.BadName(1, NameProblem.TAKEN)), state.problems)
        assertEquals(NameProblem.TAKEN, state.nameProblem(1))
        assertTrue(events.isEmpty())
    }

    @Test
    fun `deleting the twin lets the entry go`() = runTest {
        val twin = twin()

        val (state, events) = editing(
            typed(StandardPreset.USER_NAME),
            EntryEditAction.Save,
            EntryEditAction.DeleteField(1),
            EntryEditAction.DialogConfirm,
            EntryEditAction.Save,
            entryId = twin,
        )

        assertTrue(state.problems.isEmpty())
        assertEquals(listOf("1"), entries.stored.getValue(EntryId(twin)).fields.filter { it.key == "PIN" }.map { it.value })
        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }

    @Test
    fun `nothing changes while the save is under way`() = runTest {
        val (state, _) = editing(
            EntryEditAction.ValueChanged(StandardPreset.TITLE, "Changed"),
            EntryEditAction.AddField,
            initial = opener.open(DB, mail).content.let { EntryEditState(creating = false, content = it, initial = it, saving = true) },
        )

        assertEquals("Mail", state.content?.value(StandardPreset.TITLE))
        assertNull(state.dialog)
    }

    // Helpers

    private fun typed(key: String = StandardPreset.TITLE) = EntryEditAction.ValueChanged(key, "typed")

    private suspend fun TestScope.adding(name: String) =
        editing(EntryEditAction.AddField, EntryEditAction.DialogNameChanged(name), EntryEditAction.DialogConfirm)

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

    private fun filled() = arrayOf<EntryEditAction>(
        EntryEditAction.ValueChanged(StandardPreset.TITLE, "Bank"),
        EntryEditAction.ValueChanged(StandardPreset.USER_NAME, "me"),
        EntryEditAction.ValueChanged(StandardPreset.PASSWORD, "secret"),
    )

    private suspend fun TestScope.creating(vararg actions: EntryEditAction) =
        perform(groupId = group.value, entryId = null, actions = actions)

    private suspend fun TestScope.editing(
        vararg actions: EntryEditAction,
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
        actions: Array<out EntryEditAction>,
        initial: EntryEditState? = null,
    ): Pair<EntryEditState, List<MviNavEvent>> {
        val (navigator, events) = recordNavigation()
        val processor = EntryEditProcessor(DB, groupId, entryId, opener, groups, EntryEditor(entries, opener, database), presets, navigator, log)

        processor.testWithInternalState(this, initial) {
            if (initial == null) runOnCreate().join()
            actions.forEach { containerHost.action(it).join() }
            cancelAndIgnoreRemainingItems()
        }
        return processor.container.stateFlow.value to events
    }

    private fun field(key: String, value: String, protected: Boolean = false) = EntryField(key, value, protected)
}
