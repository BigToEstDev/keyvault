package ru.kino.dev.settings.pres.presetread

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.orbitmvi.orbit.test.testWithInternalState
import ru.kino.dev.core.preset.CustomPreset
import ru.kino.dev.core.preset.FakePresetRepository
import ru.kino.dev.core.preset.PresetEditor
import ru.kino.dev.core.preset.PresetFailure
import ru.kino.dev.core.preset.PresetId
import ru.kino.dev.core.preset.StandardPreset
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.PresetEditRoute
import ru.kino.dev.settings.pres.testing.RecordingLog
import ru.kino.dev.settings.pres.testing.recordNavigation
import ru.kino.dev.settings.pres.testing.runEach

class PresetReadProcessorTest {

    private val repository = FakePresetRepository(CARD)
    private val log = RecordingLog()

    @Test
    fun `the screen shown reads the preset`() = runTest {
        val (state, events) = perform(CARD_ID, { onLoad() })

        assertEquals(CARD, state.preset)
        assertTrue(state.own)
        assertEquals(emptyList(), events)
    }

    @Test
    fun `the standard preset is read at once, not one's own`() = runTest {
        val (state, _) = perform(null)

        assertEquals(StandardPreset.preset, state.preset)
        assertFalse(state.own)
    }

    @Test
    fun `a preset gone goes back`() = runTest {
        val (state, events) = perform("gone", { onLoad() })

        assertNull(state.preset)
        assertEquals(listOf<MviNavEvent>(MviNavEvent.NavigateBack), events)
    }

    @Test
    fun `edit opens its form`() = runTest {
        val (_, events) = perform(CARD_ID, { onLoad() }, { onEdit() })

        assertEquals(listOf<MviNavEvent>(MviNavEvent.NavigateTo(PresetEditRoute(CARD_ID))), events)
    }

    @Test
    fun `the standard preset is not edited`() = runTest {
        val (_, events) = perform(null, { onEdit() })

        assertEquals(emptyList(), events)
    }

    @Test
    fun `delete asks first`() = runTest {
        val (state, _) = perform(CARD_ID, { onLoad() }, { onDelete() })

        assertTrue(state.deleting)
        assertEquals(listOf(CARD), repository.presets.value)
    }

    @Test
    fun `the standard preset is not deleted`() = runTest {
        val (state, _) = perform(null, { onDelete() })

        assertFalse(state.deleting)
    }

    @Test
    fun `delete confirmed removes the preset and goes back`() = runTest {
        val (_, events) = perform(CARD_ID, { onLoad() }, { onDelete() }, { onDeleteConfirm() })

        assertEquals(emptyList(), repository.presets.value)
        assertEquals(listOf<MviNavEvent>(MviNavEvent.NavigateBack), events)
    }

    @Test
    fun `delete dismissed keeps the preset`() = runTest {
        val (state, _) = perform(CARD_ID, { onLoad() }, { onDelete() }, { onDeleteDismiss() })

        assertFalse(state.deleting)
        assertEquals(listOf(CARD), repository.presets.value)
    }

    @Test
    fun `a delete not written says so, goes to the log and stays`() = runTest {
        repository.failure = PresetFailure.NotSaved()

        val (state, events) = perform(CARD_ID, { onLoad() }, { onDelete() }, { onDeleteConfirm() })

        assertEquals(PresetReadMessage.NotDeleted, state.message)
        assertFalse(state.working)
        assertEquals(1, log.logged.size)
        assertEquals(emptyList(), events)
    }

    @Test
    fun `a preset edited beside it while it is shown is read as saved`() = runTest {
        val (navigator, _) = recordNavigation()
        val processor = PresetReadProcessor(CARD_ID, repository, PresetEditor(repository), navigator, log)
        val renamed = CustomPreset.of(PresetId(CARD_ID), "Card, renamed", listOf(listOf("Bank")))

        processor.testWithInternalState(this) {
            // Listening, as while the screen is shown
            runOnCreate()
            assertEquals(CARD, awaitInternalState().preset)

            repository.save(renamed)

            assertEquals(renamed, awaitInternalState().preset)
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `back leaves the screen`() = runTest {
        val (_, events) = perform(CARD_ID, { onBack() })

        assertEquals(listOf<MviNavEvent>(MviNavEvent.NavigateBack), events)
    }

    /** Runs [actions] one after another on a new processor of [presetId]; the state they end in, and the navigation. */
    private suspend fun TestScope.perform(
        presetId: String?,
        vararg actions: PresetReadActions.() -> Unit,
    ): Pair<PresetReadState, List<MviNavEvent>> {
        val (navigator, events) = recordNavigation()
        val processor = PresetReadProcessor(presetId, repository, PresetEditor(repository), navigator, log)

        processor.testWithInternalState(this) {
            containerHost.runEach(actions)
            cancelAndIgnoreRemainingItems()
        }
        return processor.container.stateFlow.value to events
    }

    private companion object {
        const val CARD_ID = "card"
        val CARD = CustomPreset.of(PresetId(CARD_ID), "Card", listOf(listOf("Bank")))
    }
}
