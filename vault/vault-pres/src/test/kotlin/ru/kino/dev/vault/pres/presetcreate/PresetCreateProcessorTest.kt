package ru.kino.dev.vault.pres.presetcreate

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.orbitmvi.orbit.test.testWithInternalState
import ru.kino.dev.core.preset.CustomPreset
import ru.kino.dev.core.preset.EntryPreset
import ru.kino.dev.core.preset.FakePresetRepository
import ru.kino.dev.core.preset.FieldNameProblem
import ru.kino.dev.core.preset.FieldPosition
import ru.kino.dev.core.preset.PresetEditor
import ru.kino.dev.core.preset.PresetFailure
import ru.kino.dev.core.preset.PresetId
import ru.kino.dev.core.preset.PresetNameProblem
import ru.kino.dev.core.preset.PresetProblem
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.vault.pres.testing.RecordingLog
import ru.kino.dev.vault.pres.testing.recordNavigation

class PresetCreateProcessorTest {

    private val repository = FakePresetRepository()
    private val log = RecordingLog()

    @Test
    fun `a new form is the first card alone, nothing changed`() = runTest {
        val (state, _) = perform()

        assertEquals(listOf(emptyList<String>()), state.cards)
        assertFalse(state.changed)
    }

    @Test
    fun `a field added goes last in its card, without the spaces around its name`() = runTest {
        val (state, _) = perform(
            PresetCreateAction.AddCard,
            PresetCreateAction.AddField(card = 1),
            PresetCreateAction.DialogNameChanged(" CVC "),
            PresetCreateAction.DialogConfirm,
        )

        assertEquals(listOf(emptyList(), listOf("CVC")), state.cards)
        assertNull(state.dialog)
        assertTrue(state.changed)
    }

    @Test
    fun `a field named as one every preset has stays in the dialog with the reason`() = runTest {
        val (state, _) = perform(
            PresetCreateAction.AddField(card = 0),
            PresetCreateAction.DialogNameChanged("password"),
            PresetCreateAction.DialogConfirm,
        )

        assertEquals(PresetCreateDialog.AddField(0, "password", FieldNameProblem.TAKEN), state.dialog)
    }

    @Test
    fun `a field named as one of another card stays in the dialog`() = runTest {
        val (state, _) = perform(
            *addField(0, "PIN"),
            PresetCreateAction.AddCard,
            PresetCreateAction.AddField(card = 1),
            PresetCreateAction.DialogNameChanged("pin"),
            PresetCreateAction.DialogConfirm,
        )

        assertEquals(FieldNameProblem.TAKEN, (state.dialog as PresetCreateDialog.AddField).problem)
    }

    @Test
    fun `a field renamed keeps its place, its own name no rival`() = runTest {
        val (state, _) = perform(
            *addField(0, "PIN"),
            *addField(0, "Bank"),
            PresetCreateAction.RenameField(FieldPosition(0, 0)),
            PresetCreateAction.DialogNameChanged("pin code"),
            PresetCreateAction.DialogConfirm,
        )

        assertEquals(listOf(listOf("pin code", "Bank")), state.cards)
    }

    @Test
    fun `a field deleted asks first, naming it`() = runTest {
        val (state, _) = perform(*addField(0, "PIN"), PresetCreateAction.DeleteField(FieldPosition(0, 0)))

        assertEquals(PresetCreateDialog.DeleteField(FieldPosition(0, 0), "PIN"), state.dialog)
    }

    @Test
    fun `a field deleted leaves its card`() = runTest {
        val (state, _) = perform(
            *addField(0, "PIN"),
            *addField(0, "Bank"),
            PresetCreateAction.DeleteField(FieldPosition(0, 0)),
            PresetCreateAction.DialogConfirm,
        )

        assertEquals(listOf(listOf("Bank")), state.cards)
    }

    @Test
    fun `a card added is deleted after asking, with its fields`() = runTest {
        val (state, _) = perform(
            PresetCreateAction.AddCard,
            *addField(1, "CVC"),
            PresetCreateAction.DeleteCard(1),
            PresetCreateAction.DialogConfirm,
        )

        assertEquals(listOf(emptyList<String>()), state.cards)
    }

    @Test
    fun `the first card is not deleted`() = runTest {
        val (state, _) = perform(PresetCreateAction.DeleteCard(0))

        assertNull(state.dialog)
    }

    @Test
    fun `a preset saved is made from the form and the screen goes back`() = runTest {
        val (_, events) = perform(
            PresetCreateAction.NameChanged("Card"),
            PresetCreateAction.AddCard,
            *addField(1, "CVC"),
            PresetCreateAction.Save(STANDARD),
        )

        val made = repository.presets.first().single()
        assertEquals("Card", made.name)
        assertEquals(listOf(emptyList(), listOf("CVC")), CustomPreset.cards(made))
        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }

    @Test
    fun `a preset without a name is refused, the name marked, and the screen stays`() = runTest {
        val (state, events) = perform(PresetCreateAction.Save(STANDARD))

        assertEquals(PresetNameProblem.EMPTY, state.nameProblem)
        assertFalse(state.saving)
        assertEquals(emptyList<MviNavEvent>(), events)
    }

    @Test
    fun `the name of the standard preset is taken`() = runTest {
        val (state, _) = perform(PresetCreateAction.NameChanged("стандарт"), PresetCreateAction.Save(STANDARD))

        assertEquals(PresetNameProblem.TAKEN, state.nameProblem)
    }

    @Test
    fun `typing the name takes its mark off`() = runTest {
        val (state, _) = perform(PresetCreateAction.Save(STANDARD), PresetCreateAction.NameChanged("C"))

        assertNull(state.nameProblem)
    }

    @Test
    fun `no tenth preset of one's own - said in the snackbar`() = runTest {
        repeat(9) { repository.save(CustomPreset.of(PresetId("$it"), "P$it", listOf(emptyList()))) }

        val (state, _) = perform(PresetCreateAction.NameChanged("New"), PresetCreateAction.Save(STANDARD))

        assertEquals(PresetCreateMessage.TooMany, state.message)
        assertEquals(emptyList<PresetProblem>(), state.problems)
    }

    @Test
    fun `a store that fails says so, goes to the log and keeps the form`() = runTest {
        repository.failure = PresetFailure.NotSaved()

        val (state, _) = perform(PresetCreateAction.NameChanged("Card"), PresetCreateAction.Save(STANDARD))

        assertEquals(PresetCreateMessage.NotSaved, state.message)
        assertEquals("Card", state.name)
        assertEquals(1, log.logged.size)
    }

    @Test
    fun `back from an untouched form leaves at once`() = runTest {
        val (_, events) = perform(PresetCreateAction.Back)

        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }

    @Test
    fun `back from a changed form asks first, and leave goes without saving`() = runTest {
        val (_, events) = perform(
            PresetCreateAction.NameChanged("Card"),
            PresetCreateAction.Back,
            PresetCreateAction.DialogConfirm,
        )

        assertEquals(listOf(MviNavEvent.NavigateBack), events)
        assertEquals(emptyList<EntryPreset>(), repository.presets.first())
    }

    private fun addField(card: Int, name: String) = arrayOf(
        PresetCreateAction.AddField(card),
        PresetCreateAction.DialogNameChanged(name),
        PresetCreateAction.DialogConfirm,
    )

    /** Runs [actions] one after another on a new processor; the state they end in, and the navigation. */
    private suspend fun TestScope.perform(vararg actions: PresetCreateAction): Pair<PresetCreateState, List<MviNavEvent>> {
        val (navigator, events) = recordNavigation()
        var ids = 0
        val processor = PresetCreateProcessor(PresetEditor(repository, newId = { PresetId("id-${++ids}") }), navigator, log)

        processor.testWithInternalState(this) {
            actions.forEach { containerHost.action(it).join() }
            cancelAndIgnoreRemainingItems()
        }
        return processor.container.stateFlow.value to events
    }

    private companion object {
        const val STANDARD = "Стандарт"
    }
}
