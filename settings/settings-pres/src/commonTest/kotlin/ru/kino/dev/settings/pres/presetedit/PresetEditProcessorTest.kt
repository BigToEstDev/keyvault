package ru.kino.dev.settings.pres.presetedit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.orbitmvi.orbit.test.testWithInternalState
import ru.kino.dev.core.FieldType
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
import ru.kino.dev.settings.pres.testing.runEach

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
            { onAddCard() },
            { onAddField(card = 1) },
            { onDialogNameChange(" CVC ") },
            { onDialogConfirm() },
        )

        assertEquals(listOf(emptyList(), listOf("CVC")), state.cards)
        assertNull(state.dialog)
        assertTrue(state.changed)
    }

    @Test
    fun `a field named as one every preset has stays in the dialog with the reason`() = runTest {
        val (state, _) = perform(
            { onAddField(card = 0) },
            { onDialogNameChange("password") },
            { onDialogConfirm() },
        )

        assertEquals(PresetEditDialog.AddField(0, "password", problem = FieldNameProblem.TAKEN), state.dialog)
    }

    @Test
    fun `a field named as one of another card stays in the dialog`() = runTest {
        val (state, _) = perform(
            *addField(0, "PIN"),
            { onAddCard() },
            { onAddField(card = 1) },
            { onDialogNameChange("pin") },
            { onDialogConfirm() },
        )

        assertEquals(FieldNameProblem.TAKEN, (state.dialog as PresetEditDialog.AddField).problem)
    }

    @Test
    fun `a field renamed keeps its place, its own name no rival`() = runTest {
        val (state, _) = perform(
            *addField(0, "PIN"),
            *addField(0, "Bank"),
            { onEditField(FieldPosition(0, 0)) },
            { onDialogNameChange("pin code") },
            { onDialogConfirm() },
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
        val (state, _) = perform(*addField(0, "PIN", protected = true), { onEditField(FieldPosition(0, 0)) })

        assertEquals(PresetEditDialog.EditField(FieldPosition(0, 0), "PIN", protected = true, type = null), state.dialog)
    }

    @Test
    fun `a field renamed keeps its protection under the new name`() = runTest {
        val (state, _) = perform(
            *addField(0, "PIN", protected = true),
            { onEditField(FieldPosition(0, 0)) },
            { onDialogNameChange("Code") },
            { onDialogConfirm() },
        )

        assertEquals(setOf("Code"), state.protectedKeys)
    }

    @Test
    fun `a field edited takes the protection switched`() = runTest {
        val (state, _) = perform(
            *addField(0, "PIN", protected = true),
            { onEditField(FieldPosition(0, 0)) },
            { onDialogProtectedChange(false) },
            { onDialogConfirm() },
        )

        assertEquals(listOf(listOf("PIN")), state.cards)
        assertTrue(state.protectedKeys.isEmpty())
    }

    @Test
    fun `a field deleted takes its protection with it`() = runTest {
        val (state, _) = perform(
            *addField(0, "PIN", protected = true),
            { onDeleteField(FieldPosition(0, 0)) },
            { onDialogConfirm() },
        )

        assertTrue(state.protectedKeys.isEmpty())
    }

    @Test
    fun `a card deleted takes the protection of its fields with it`() = runTest {
        val (state, _) = perform(
            { onAddCard() },
            *addField(1, "CVC", protected = true),
            { onDeleteCard(1) },
            { onDialogConfirm() },
        )

        assertTrue(state.protectedKeys.isEmpty())
    }

    @Test
    fun `a field deleted asks first, naming it`() = runTest {
        val (state, _) = perform(*addField(0, "PIN"), { onDeleteField(FieldPosition(0, 0)) })

        assertEquals(PresetEditDialog.DeleteField(FieldPosition(0, 0), "PIN"), state.dialog)
    }

    @Test
    fun `a field deleted leaves its card`() = runTest {
        val (state, _) = perform(
            *addField(0, "PIN"),
            *addField(0, "Bank"),
            { onDeleteField(FieldPosition(0, 0)) },
            { onDialogConfirm() },
        )

        assertEquals(listOf(listOf("Bank")), state.cards)
    }

    @Test
    fun `a full card takes no more fields, a field deleted makes room again`() = runTest {
        val full = (1..10).flatMap { addField(0, "F$it").toList() }.toTypedArray()

        val (state, _) = perform(*full, { onAddField(card = 0) })

        assertFalse(state.canAddField(0))
        assertNull(state.dialog)

        val (after, _) = perform(
            *full,
            { onDeleteField(FieldPosition(0, 0)) },
            { onDialogConfirm() },
        )

        assertTrue(after.canAddField(0))
    }

    @Test
    fun `a preset of 6 cards takes no more`() = runTest {
        val (state, _) = perform(*Array(6) { { onAddCard() } })

        assertEquals(6, state.cards.size)
        assertFalse(state.canAddCard)
    }

    @Test
    fun `a card deleted from a full preset makes room again`() = runTest {
        val (state, _) = perform(
            *Array(5) { { onAddCard() } },
            { onDeleteCard(1) },
            { onDialogConfirm() },
        )

        assertEquals(5, state.cards.size)
        assertTrue(state.canAddCard)
    }

    @Test
    fun `a card added is deleted after asking, with its fields`() = runTest {
        val (state, _) = perform(
            { onAddCard() },
            *addField(1, "CVC"),
            { onDeleteCard(1) },
            { onDialogConfirm() },
        )

        assertEquals(listOf(emptyList<String>()), state.cards)
    }

    @Test
    fun `the first card is not deleted`() = runTest {
        val (state, _) = perform({ onDeleteCard(0) })

        assertNull(state.dialog)
    }

    @Test
    fun `a preset saved is made from the form and the screen goes back`() = runTest {
        val (_, events) = perform(
            { onNameChange("Card") },
            { onAddCard() },
            *addField(1, "CVC"),
            { onSave(STANDARD) },
        )

        val made = repository.presets.first().single()
        assertEquals("Card", made.name)
        assertEquals(listOf(emptyList(), listOf("CVC")), CustomPreset.cards(made))
        assertEquals(listOf<MviNavEvent>(MviNavEvent.NavigateBack), events)
    }

    @Test
    fun `a preset without a name is refused, the name marked, and the screen stays`() = runTest {
        val (state, events) = perform({ onSave(STANDARD) })

        assertEquals(PresetNameProblem.EMPTY, state.nameProblem)
        assertFalse(state.saving)
        assertEquals(emptyList<MviNavEvent>(), events)
    }

    @Test
    fun `the name of the standard preset is taken`() = runTest {
        val (state, _) = perform({ onNameChange("стандарт") }, { onSave(STANDARD) })

        assertEquals(PresetNameProblem.TAKEN, state.nameProblem)
    }

    @Test
    fun `typing the name takes its mark off`() = runTest {
        val (state, _) = perform({ onSave(STANDARD) }, { onNameChange("C") })

        assertNull(state.nameProblem)
    }

    @Test
    fun `no tenth preset of one's own - said in the snackbar`() = runTest {
        repeat(9) { repository.save(CustomPreset.of(PresetId("$it"), "P$it", listOf(emptyList()))) }

        val (state, _) = perform({ onNameChange("New") }, { onSave(STANDARD) })

        assertEquals(PresetEditMessage.TooMany, state.message)
        assertEquals(emptyList<PresetProblem>(), state.problems)
    }

    @Test
    fun `a store that fails says so, goes to the log and keeps the form`() = runTest {
        repository.failure = PresetFailure.NotSaved()

        val (state, _) = perform({ onNameChange("Card") }, { onSave(STANDARD) })

        assertEquals(PresetEditMessage.NotSaved, state.message)
        assertEquals("Card", state.name)
        assertEquals(1, log.logged.size)
    }

    @Test
    fun `back from an untouched form leaves at once`() = runTest {
        val (_, events) = perform({ onBack() })

        assertEquals(listOf<MviNavEvent>(MviNavEvent.NavigateBack), events)
    }

    @Test
    fun `back from a changed form asks first, and leave goes without saving`() = runTest {
        val (_, events) = perform(
            { onNameChange("Card") },
            { onBack() },
            { onDialogConfirm() },
        )

        assertEquals(listOf<MviNavEvent>(MviNavEvent.NavigateBack), events)
        assertEquals(emptyList<EntryPreset>(), repository.presets.first())
    }

    @Test
    fun `a preset edited is read into the form, nothing changed`() = runTest {
        repository.save(CARD)

        val (state, _) = perform({ onLoad() }, presetId = CARD_ID)

        assertTrue(state.editing)
        assertFalse(state.loading)
        assertEquals("Card", state.name)
        assertEquals(listOf(listOf("Bank"), listOf("CVC")), state.cards)
        assertFalse(state.changed)
    }

    @Test
    fun `a preset gone before it is read leaves at once`() = runTest {
        val (_, events) = perform({ onLoad() }, presetId = CARD_ID)

        assertEquals(listOf<MviNavEvent>(MviNavEvent.NavigateBack), events)
    }

    @Test
    fun `an edit that adds a field saves under the same id and goes back`() = runTest {
        repository.save(CARD)

        val (_, events) = perform({ onLoad() }, *addField(1, "PIN"), { onSave(STANDARD) }, presetId = CARD_ID)

        val saved = repository.presets.first().single()
        assertEquals(PresetId(CARD_ID), saved.id)
        assertEquals(listOf(listOf("Bank"), listOf("CVC", "PIN")), CustomPreset.cards(saved))
        assertEquals(listOf<MviNavEvent>(MviNavEvent.NavigateBack), events)
    }

    @Test
    fun `an edit that drops a field asks first and saves nothing yet`() = runTest {
        repository.save(CARD)

        val (state, events) = perform(
            { onLoad() },
            { onDeleteCard(1) },
            { onDialogConfirm() },
            { onSave(STANDARD) },
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
            { onLoad() },
            { onEditField(FieldPosition(0, 0)) },
            { onDialogNameChange("Issuer") },
            { onDialogConfirm() },
            { onSave(STANDARD) },
            { onDialogConfirm() },
            presetId = CARD_ID,
        )

        assertEquals(listOf(listOf("Issuer"), listOf("CVC")), CustomPreset.cards(repository.presets.first().single()))
        assertEquals(listOf<MviNavEvent>(MviNavEvent.NavigateBack), events)
    }

    @Test
    fun `an edit reads the protection of the fields, nothing changed`() = runTest {
        repository.save(CARD_PROTECTED)

        val (state, _) = perform({ onLoad() }, presetId = CARD_ID)

        assertEquals(setOf("CVC"), state.protectedKeys)
        assertFalse(state.changed)
    }

    @Test
    fun `an edit that switches the protection saves it without asking - no field is dropped`() = runTest {
        repository.save(CARD)

        val (_, events) = perform(
            { onLoad() },
            { onEditField(FieldPosition(1, 0)) },
            { onDialogProtectedChange(true) },
            { onDialogConfirm() },
            { onSave(STANDARD) },
            presetId = CARD_ID,
        )

        assertEquals(setOf("CVC"), CustomPreset.protectedKeys(repository.presets.first().single()))
        assertEquals(listOf<MviNavEvent>(MviNavEvent.NavigateBack), events)
    }

    // The types of the fields (pass-docs, Step 75, item 3) - "card" is one this app does not know: a newer client wrote it

    @Test
    fun `an edit reads the types of the fields, nothing changed`() = runTest {
        repository.save(CARD_TYPED)

        val (state, _) = perform({ onLoad() }, { onEditField(FieldPosition(1, 0)) }, presetId = CARD_ID)

        assertEquals(mapOf("CVC" to "card"), state.fieldTypes)
        assertEquals(PresetEditDialog.EditField(FieldPosition(1, 0), "CVC", protected = false, type = "card"), state.dialog)
        assertFalse(state.changed)
    }

    @Test
    fun `a field renamed keeps its type under the new name, and it is saved`() = runTest {
        repository.save(CARD_TYPED)

        perform(
            { onLoad() },
            { onEditField(FieldPosition(1, 0)) },
            { onDialogNameChange("Code") },
            { onDialogConfirm() },
            { onSave(STANDARD) },
            // The old name is dropped from the preset: it asks first
            { onDialogConfirm() },
            presetId = CARD_ID,
        )

        assertEquals(mapOf("Code" to "card"), CustomPreset.fieldTypes(repository.presets.first().single()))
    }

    @Test
    fun `text picked takes the type off the field and is a change`() = runTest {
        repository.save(CARD_TYPED)

        val (state, _) = perform(
            { onLoad() },
            { onEditField(FieldPosition(1, 0)) },
            { onDialogTypeChange(FieldType.TEXT) },
            { onDialogConfirm() },
            presetId = CARD_ID,
        )

        assertTrue(state.fieldTypes.isEmpty())
        assertTrue(state.changed)
    }

    @Test
    fun `a deleted card takes the types of its fields`() = runTest {
        repository.save(CARD_TYPED)

        val (state, _) = perform(
            { onLoad() },
            { onDeleteCard(1) },
            { onDialogConfirm() },
            presetId = CARD_ID,
        )

        assertTrue(state.fieldTypes.isEmpty())
    }

    @Test
    fun `a preset deleted elsewhere before the save is said in the snackbar`() = runTest {
        repository.save(CARD)
        repository.failure = PresetFailure.NotFound()

        val (state, events) = perform({ onLoad() }, *addField(1, "PIN"), { onSave(STANDARD) }, presetId = CARD_ID)

        assertEquals(PresetEditMessage.Gone, state.message)
        assertFalse(state.saving)
        assertEquals(emptyList<MviNavEvent>(), events)
        assertEquals(0, log.logged.size)
    }

    private fun addField(card: Int, name: String, protected: Boolean = false) = arrayOf<PresetEditActions.() -> Unit>(
        { onAddField(card) },
        { onDialogNameChange(name) },
        { onDialogProtectedChange(protected) },
        { onDialogConfirm() },
    )

    /** Runs [actions] one after another on a new processor; the state they end in, and the navigation. */
    private suspend fun TestScope.perform(
        vararg actions: PresetEditActions.() -> Unit,
        presetId: String? = null,
    ): Pair<PresetEditState, List<MviNavEvent>> {
        val (navigator, events) = recordNavigation()
        var ids = 0
        val editor = PresetEditor(repository, newId = { PresetId("id-${++ids}") })
        val processor = PresetEditProcessor(presetId, repository, editor, navigator, log)

        processor.testWithInternalState(this) {
            containerHost.runEach(actions)
            cancelAndIgnoreRemainingItems()
        }
        return processor.container.stateFlow.value to events
    }

    private companion object {
        const val STANDARD = "Стандарт"
        const val CARD_ID = "card"
        val CARD = CustomPreset.of(PresetId(CARD_ID), "Card", listOf(listOf("Bank"), listOf("CVC")))
        val CARD_PROTECTED = CustomPreset.of(PresetId(CARD_ID), "Card", listOf(listOf("Bank"), listOf("CVC")), setOf("CVC"))
        val CARD_TYPED = CustomPreset.of(
            PresetId(CARD_ID),
            "Card",
            listOf(listOf("Bank"), listOf("CVC")),
            fieldTypes = mapOf("CVC" to "card"),
        )
    }
}
