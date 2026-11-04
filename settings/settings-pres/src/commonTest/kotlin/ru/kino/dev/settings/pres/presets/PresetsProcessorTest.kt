package ru.kino.dev.settings.pres.presets

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
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
import ru.kino.dev.settings.pres.navigation.PresetReadRoute
import ru.kino.dev.settings.pres.testing.runEach
import ru.kino.dev.settings.presets.FakePresetSettingsRepository

class PresetsProcessorTest {

    private val repository = FakePresetRepository(CARD)
    private val log = RecordingLog()

    @Test
    fun `the screen shown reads the switch and the presets`() = runTest {
        val (state, _) = perform(FakePresetSettingsRepository(usePresets = false), { onLoad() })

        assertEquals(false, state.usePresets)
        assertEquals(listOf(CARD), state.presets)
    }

    @Test
    fun `presets switched off are kept at once`() = runTest {
        val settings = FakePresetSettingsRepository()

        val (state, _) = perform(settings, { onLoad() }, { onSetUsePresets(false) })

        assertEquals(false, state.usePresets)
        assertEquals(listOf(false), settings.savedUsePresets)
    }

    @Test
    fun `presets switch not written shows what is kept, with a message`() = runTest {
        val (state, _) = perform(
            FakePresetSettingsRepository(failSave = true),
            { onLoad() },
            { onSetUsePresets(false) },
        )

        assertEquals(true, state.usePresets)
        assertEquals(PresetsMessage.NotSaved, state.message)
    }

    @Test
    fun `create opens a new preset`() = runTest {
        val (_, events) = perform(FakePresetSettingsRepository(), { onLoad() }, { onCreate() })

        assertEquals(listOf<MviNavEvent>(MviNavEvent.NavigateTo(PresetEditRoute(readSaved = true))), events)
    }

    @Test
    fun `no create past the most presets a person has`() = runTest {
        repeat(8) { repository.save(CustomPreset.of(PresetId("$it"), "P$it", listOf(emptyList()))) }

        val (state, events) = perform(FakePresetSettingsRepository(), { onLoad() }, { onCreate() })

        assertFalse(state.canCreate)
        assertEquals(emptyList<MviNavEvent>(), events)
    }

    @Test
    fun `the new presets open beside the list take their places`() = runTest {
        repeat(6) { repository.save(CustomPreset.of(PresetId("$it"), "P$it", listOf(emptyList()))) }

        val (state, _) = perform(FakePresetSettingsRepository(), { onLoad() }, { onDrafts(1) })

        assertTrue(state.canCreate)
    }

    @Test
    fun `no create past the most presets, the new ones open beside the list counted`() = runTest {
        repeat(6) { repository.save(CustomPreset.of(PresetId("$it"), "P$it", listOf(emptyList()))) }

        val (state, events) = perform(FakePresetSettingsRepository(), { onLoad() }, { onDrafts(2) }, { onCreate() })

        assertFalse(state.canCreate)
        assertEquals(emptyList<MviNavEvent>(), events)
    }

    @Test
    fun `a preset tapped is read`() = runTest {
        val (_, events) = perform(FakePresetSettingsRepository(), { onLoad() }, { onOpen(PresetId(CARD_ID)) })

        assertEquals(listOf<MviNavEvent>(MviNavEvent.NavigateTo(PresetReadRoute(CARD_ID))), events)
    }

    @Test
    fun `the standard preset tapped is read`() = runTest {
        val (_, events) = perform(FakePresetSettingsRepository(), { onLoad() }, { onOpen(null) })

        assertEquals(listOf<MviNavEvent>(MviNavEvent.NavigateTo(PresetReadRoute(null))), events)
    }

    @Test
    fun `edit opens the preset`() = runTest {
        val (_, events) = perform(FakePresetSettingsRepository(), { onLoad() }, { onEdit(CARD.id!!) })

        assertEquals(listOf<MviNavEvent>(MviNavEvent.NavigateTo(PresetEditRoute(CARD_ID))), events)
    }

    @Test
    fun `delete asks first`() = runTest {
        val (state, _) = perform(FakePresetSettingsRepository(), { onLoad() }, { onDelete(CARD) })

        assertEquals(CARD, state.deleting)
        assertEquals(listOf(CARD), repository.presets.first())
    }

    @Test
    fun `the standard preset is not deleted`() = runTest {
        val (state, _) = perform(
            FakePresetSettingsRepository(),
            { onLoad() },
            { onDelete(StandardPreset.preset) },
        )

        assertNull(state.deleting)
    }

    @Test
    fun `delete confirmed removes the preset from the list`() = runTest {
        val (state, _) = perform(
            FakePresetSettingsRepository(),
            { onLoad() },
            { onDelete(CARD) },
            { onDeleteConfirm() },
        )

        assertNull(state.deleting)
        assertEquals(emptyList(), state.presets)
        assertEquals(emptyList<Any>(), repository.presets.first())
    }

    @Test
    fun `a delete not written says so, goes to the log and keeps the preset`() = runTest {
        // Reading is not failed by the fake - only changes are
        repository.failure = PresetFailure.NotSaved()

        val (state, _) = perform(
            FakePresetSettingsRepository(),
            { onLoad() },
            { onDelete(CARD) },
            { onDeleteConfirm() },
        )

        assertEquals(PresetsMessage.NotDeleted, state.message)
        assertEquals(listOf(CARD), state.presets)
        assertEquals(1, log.logged.size)
    }

    @Test
    fun `a preset made beside the list while it is shown is in it`() = runTest {
        val (navigator, _) = recordNavigation()
        val processor = PresetsProcessor(FakePresetSettingsRepository(), repository, PresetEditor(repository), navigator, log)
        val made = CustomPreset.of(PresetId("site"), "Site", listOf(emptyList()))

        processor.testWithInternalState(this) {
            // Listening, as while the screen is shown
            runOnCreate()
            assertEquals(listOf(CARD), awaitInternalState().presets)

            // Saved in its tab, beside the list
            repository.save(made)

            assertEquals(listOf(CARD, made), awaitInternalState().presets)
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `back leaves the screen`() = runTest {
        val (_, events) = perform(FakePresetSettingsRepository(), { onBack() })

        assertEquals(listOf<MviNavEvent>(MviNavEvent.NavigateBack), events)
    }

    /** Runs [actions] one after another on a new processor; the state they end in, and the navigation. */
    private suspend fun TestScope.perform(
        settings: FakePresetSettingsRepository,
        vararg actions: PresetsActions.() -> Unit,
    ): Pair<PresetsState, List<MviNavEvent>> {
        val (navigator, events) = recordNavigation()
        val processor = PresetsProcessor(settings, repository, PresetEditor(repository), navigator, log)

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
