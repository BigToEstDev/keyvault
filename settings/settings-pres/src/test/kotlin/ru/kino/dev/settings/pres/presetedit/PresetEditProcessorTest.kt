package ru.kino.dev.settings.pres.presetedit

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
import ru.kino.dev.settings.pres.testing.RecordingLog
import ru.kino.dev.settings.pres.testing.recordNavigation

class PresetEditProcessorTest {

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
            PresetEditAction.AddCard,
            PresetEditAction.AddField(card = 1),
            PresetEditAction.DialogNameChanged(" CVC "),
            PresetEditAction.DialogConfirm,
        )

        assertEquals(listOf(emptyList(), listOf("CVC")), state.cards)
        assertNull(state.dialog)
        assertTrue(state.changed)
    }

    @Test
    fun `a field named as one every preset has stays in the dialog with the reason`() = runTest {
        val (state, _) = perform(
            PresetEditAction.AddField(card = 0),
            PresetEditAction.DialogNameChanged("password"),
            PresetEditAction.DialogConfirm,
        )

        assertEquals(PresetEditDialog.AddField(0, "password", problem = FieldNameProblem.TAKEN), state.dialog)
    }

    @Test
    fun `a field named as one of another card stays in the dialog`() = runTest {
        val (state, _) = perform(
            *addField(0, "PIN"),
            PresetEditAction.AddCard,
            PresetEditAction.AddField(card = 1),
            PresetEditAction.DialogNameChanged("pin"),
            PresetEditAction.DialogConfirm,
        )

        assertEquals(FieldNameProblem.TAKEN, (state.dialog as PresetEditDialog.AddField).problem)
    }

    @Test
    fun `a field renamed keeps its place, its own name no rival`() = runTest {
        val (state, _) = perform(
            *addField(0, "PIN"),
            *addField(0, "Bank"),
            PresetEditAction.EditField(FieldPosition(0, 0)),
            PresetEditAction.DialogNameChanged("pin code"),
            PresetEditAction.DialogConfirm,
        )

        assertEquals(listOf(listOf("pin code", "Bank")), state.cards)
    }

    @Test
    fun `a field added is not protected unless switched`() = runTest {
        val (state, _) = perform(*addField(0, "PIN"))

        assertTrue(state.protectedKeys.isEmpty())
    }

    @Test
    fun `a field added switched to protected is protected`() = runTest {
        val (state, _) = perform(*addField(0, "PIN", protected = true))

        assertEquals(setOf("PIN"), state.protectedKeys)
        assertTrue(state.changed)
    }

    @Test
    fun `edit opens with the name and the protection of the field`() = runTest {
        val (state, _) = perform(*addField(0, "PIN", protected = true), PresetEditAction.EditField(FieldPosition(0, 0)))

        assertEquals(PresetEditDialog.EditField(FieldPosition(0, 0), "PIN", protected = true), state.dialog)
    }

    @Test
    fun `a field renamed keeps its protection under the new name`() = runTest {
        val (state, _) = perform(
            *addField(0, "PIN", protected = true),
            PresetEditAction.EditField(FieldPosition(0, 0)),
            PresetEditAction.DialogNameChanged("Code"),
            PresetEditAction.DialogConfirm,
        )

        assertEquals(setOf("Code"), state.protectedKeys)
    }

    @Test
    fun `a field edited takes the protection switched`() = runTest {
        val (state, _) = perform(
            *addField(0, "PIN", protected = true),
            PresetEditAction.EditField(FieldPosition(0, 0)),
            PresetEditAction.DialogProtectedChanged(false),
            PresetEditAction.DialogConfirm,
        )

        assertEquals(listOf(listOf("PIN")), state.cards)
        assertTrue(state.protectedKeys.isEmpty())
    }

    @Test
    fun `a field deleted takes its protection with it`() = runTest {
        val (state, _) = perform(
            *addField(0, "PIN", protected = true),
            PresetEditAction.DeleteField(FieldPosition(0, 0)),
            PresetEditAction.DialogConfirm,
        )

        assertTrue(state.protectedKeys.isEmpty())
    }

    @Test
    fun `a card deleted takes the protection of its fields with it`() = runTest {
        val (state, _) = perform(
            PresetEditAction.AddCard,
            *addField(1, "CVC", protected = true),
            PresetEditAction.DeleteCard(1),
            PresetEditAction.DialogConfirm,
        )

        assertTrue(state.protectedKeys.isEmpty())
    }

    @Test
    fun `a field deleted asks first, naming it`() = runTest {
        val (state, _) = perform(*addField(0, "PIN"), PresetEditAction.DeleteField(FieldPosition(0, 0)))

        assertEquals(PresetEditDialog.DeleteField(FieldPosition(0, 0), "PIN"), state.dialog)
    }

    @Test
    fun `a field deleted leaves its card`() = runTest {
        val (state, _) = perform(
            *addField(0, "PIN"),
            *addField(0, "Bank"),
            PresetEditAction.DeleteField(FieldPosition(0, 0)),
            PresetEditAction.DialogConfirm,
        )

        assertEquals(listOf(listOf("Bank")), state.cards)
    }

    @Test
    fun `a full card takes no more fields, a field deleted makes room again`() = runTest {
        val full = (1..10).flatMap { addField(0, "F$it").toList() }.toTypedArray()

        val (state, _) = perform(*full, PresetEditAction.AddField(card = 0))

        assertFalse(state.canAddField(0))
        assertNull(state.dialog)

        val (after, _) = perform(
            *full,
            PresetEditAction.DeleteField(FieldPosition(0, 0)),
            PresetEditAction.DialogConfirm,
        )

        assertTrue(after.canAddField(0))
    }

    @Test
    fun `a preset of 6 cards takes no more`() = runTest {
        val (state, _) = perform(*Array(6) { PresetEditAction.AddCard })

        assertEquals(6, state.cards.size)
        assertFalse(state.canAddCard)
    }

    @Test
    fun `a card deleted from a full preset makes room again`() = runTest {
        val (state, _) = perform(
            *Array(5) { PresetEditAction.AddCard },
            PresetEditAction.DeleteCard(1),
            PresetEditAction.DialogConfirm,
        )

        assertEquals(5, state.cards.size)
        assertTrue(state.canAddCard)
    }

    @Test
    fun `a card added is deleted after asking, with its fields`() = runTest {
        val (state, _) = perform(
            PresetEditAction.AddCard,
            *addField(1, "CVC"),
            PresetEditAction.DeleteCard(1),
            PresetEditAction.DialogConfirm,
        )

        assertEquals(listOf(emptyList<String>()), state.cards)
    }

    @Test
    fun `the first card is not deleted`() = runTest {
        val (state, _) = perform(PresetEditAction.DeleteCard(0))

        assertNull(state.dialog)
    }

    @Test
    fun `a preset saved is made from the form and the screen goes back`() = runTest {
        val (_, events) = perform(
            PresetEditAction.NameChanged("Card"),
            PresetEditAction.AddCard,
            *addField(1, "CVC"),
            PresetEditAction.Save(STANDARD),
        )

        val made = repository.presets.first().single()
        assertEquals("Card", made.name)
        assertEquals(listOf(emptyList(), listOf("CVC")), CustomPreset.cards(made))
        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }

    @Test
    fun `a preset without a name is refused, the name marked, and the screen stays`() = runTest {
        val (state, events) = perform(PresetEditAction.Save(STANDARD))

        assertEquals(PresetNameProblem.EMPTY, state.nameProblem)
        assertFalse(state.saving)
        assertEquals(emptyList<MviNavEvent>(), events)
    }

    @Test
    fun `the name of the standard preset is taken`() = runTest {
        val (state, _) = perform(PresetEditAction.NameChanged("стандарт"), PresetEditAction.Save(STANDARD))

        assertEquals(PresetNameProblem.TAKEN, state.nameProblem)
    }

    @Test
    fun `typing the name takes its mark off`() = runTest {
        val (state, _) = perform(PresetEditAction.Save(STANDARD), PresetEditAction.NameChanged("C"))

        assertNull(state.nameProblem)
    }

    @Test
    fun `no tenth preset of one's own - said in the snackbar`() = runTest {
        repeat(9) { repository.save(CustomPreset.of(PresetId("$it"), "P$it", listOf(emptyList()))) }

        val (state, _) = perform(PresetEditAction.NameChanged("New"), PresetEditAction.Save(STANDARD))

        assertEquals(PresetEditMessage.TooMany, state.message)
        assertEquals(emptyList<PresetProblem>(), state.problems)
    }

    @Test
    fun `a store that fails says so, goes to the log and keeps the form`() = runTest {
        repository.failure = PresetFailure.NotSaved()

        val (state, _) = perform(PresetEditAction.NameChanged("Card"), PresetEditAction.Save(STANDARD))

        assertEquals(PresetEditMessage.NotSaved, state.message)
        assertEquals("Card", state.name)
        assertEquals(1, log.logged.size)
    }

    @Test
    fun `back from an untouched form leaves at once`() = runTest {
        val (_, events) = perform(PresetEditAction.Back)

        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }

    @Test
    fun `back from a changed form asks first, and leave goes without saving`() = runTest {
        val (_, events) = perform(
            PresetEditAction.NameChanged("Card"),
            PresetEditAction.Back,
            PresetEditAction.DialogConfirm,
        )

        assertEquals(listOf(MviNavEvent.NavigateBack), events)
        assertEquals(emptyList<EntryPreset>(), repository.presets.first())
    }

    @Test
    fun `a preset edited is read into the form, nothing changed`() = runTest {
        repository.save(CARD)

        val (state, _) = perform(PresetEditAction.Load, presetId = CARD_ID)

        assertTrue(state.editing)
        assertFalse(state.loading)
        assertEquals("Card", state.name)
        assertEquals(listOf(listOf("Bank"), listOf("CVC")), state.cards)
        assertFalse(state.changed)
    }

    @Test
    fun `a preset gone before it is read leaves at once`() = runTest {
        val (_, events) = perform(PresetEditAction.Load, presetId = CARD_ID)

        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }

    @Test
    fun `an edit that adds a field saves under the same id and goes back`() = runTest {
        repository.save(CARD)

        val (_, events) = perform(PresetEditAction.Load, *addField(1, "PIN"), PresetEditAction.Save(STANDARD), presetId = CARD_ID)

        val saved = repository.presets.first().single()
        assertEquals(PresetId(CARD_ID), saved.id)
        assertEquals(listOf(listOf("Bank"), listOf("CVC", "PIN")), CustomPreset.cards(saved))
        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }

    @Test
    fun `an edit that drops a field asks first and saves nothing yet`() = runTest {
        repository.save(CARD)

        val (state, events) = perform(
            PresetEditAction.Load,
            PresetEditAction.DeleteCard(1),
            PresetEditAction.DialogConfirm,
            PresetEditAction.Save(STANDARD),
            presetId = CARD_ID,
        )

        assertEquals(PresetEditDialog.FieldsDropped(STANDARD), state.dialog)
        assertEquals(listOf(CARD), repository.presets.first())
        assertEquals(emptyList<MviNavEvent>(), events)
    }

    @Test
    fun `an edit that drops a field saves once agreed`() = runTest {
        repository.save(CARD)

        val (_, events) = perform(
            PresetEditAction.Load,
            PresetEditAction.EditField(FieldPosition(0, 0)),
            PresetEditAction.DialogNameChanged("Issuer"),
            PresetEditAction.DialogConfirm,
            PresetEditAction.Save(STANDARD),
            PresetEditAction.DialogConfirm,
            presetId = CARD_ID,
        )

        assertEquals(listOf(listOf("Issuer"), listOf("CVC")), CustomPreset.cards(repository.presets.first().single()))
        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }

    @Test
    fun `an edit reads the protection of the fields, nothing changed`() = runTest {
        repository.save(CARD_PROTECTED)

        val (state, _) = perform(PresetEditAction.Load, presetId = CARD_ID)

        assertEquals(setOf("CVC"), state.protectedKeys)
        assertFalse(state.changed)
    }

    @Test
    fun `an edit that switches the protection saves it without asking - no field is dropped`() = runTest {
        repository.save(CARD)

        val (_, events) = perform(
            PresetEditAction.Load,
            PresetEditAction.EditField(FieldPosition(1, 0)),
            PresetEditAction.DialogProtectedChanged(true),
            PresetEditAction.DialogConfirm,
            PresetEditAction.Save(STANDARD),
            presetId = CARD_ID,
        )

        assertEquals(setOf("CVC"), CustomPreset.protectedKeys(repository.presets.first().single()))
        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }

    @Test
    fun `a preset deleted elsewhere before the save is said in the snackbar`() = runTest {
        repository.save(CARD)
        repository.failure = PresetFailure.NotFound()

        val (state, events) = perform(PresetEditAction.Load, *addField(1, "PIN"), PresetEditAction.Save(STANDARD), presetId = CARD_ID)

        assertEquals(PresetEditMessage.Gone, state.message)
        assertFalse(state.saving)
        assertEquals(emptyList<MviNavEvent>(), events)
        assertEquals(0, log.logged.size)
    }

    private fun addField(card: Int, name: String, protected: Boolean = false) = arrayOf(
        PresetEditAction.AddField(card),
        PresetEditAction.DialogNameChanged(name),
        PresetEditAction.DialogProtectedChanged(protected),
        PresetEditAction.DialogConfirm,
    )

    /** Runs [actions] one after another on a new processor; the state they end in, and the navigation. */
    private suspend fun TestScope.perform(
        vararg actions: PresetEditAction,
        presetId: String? = null,
    ): Pair<PresetEditState, List<MviNavEvent>> {
        val (navigator, events) = recordNavigation()
        var ids = 0
        val editor = PresetEditor(repository, newId = { PresetId("id-${++ids}") })
        val processor = PresetEditProcessor(presetId, repository, editor, navigator, log)

        processor.testWithInternalState(this) {
            actions.forEach { containerHost.action(it).join() }
            cancelAndIgnoreRemainingItems()
        }
        return processor.container.stateFlow.value to events
    }

    private companion object {
        const val STANDARD = "Стандарт"
        const val CARD_ID = "card"
        val CARD = CustomPreset.of(PresetId(CARD_ID), "Card", listOf(listOf("Bank"), listOf("CVC")))
        val CARD_PROTECTED = CustomPreset.of(PresetId(CARD_ID), "Card", listOf(listOf("Bank"), listOf("CVC")), setOf("CVC"))
    }
}
