package ru.kino.dev.settings.pres.presetscontainer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.orbitmvi.orbit.test.testWithInternalState
import ru.kino.dev.core.preset.CustomPreset
import ru.kino.dev.core.preset.FakePresetRepository
import ru.kino.dev.core.preset.PresetId
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.PresetEditRoute
import ru.kino.dev.navigation.SettingsRoute
import ru.kino.dev.settings.pres.testing.runEach

class PresetsContainerProcessorTest {

    /** What the routers answered, in order: true - taken by the container. */
    private val taken = mutableListOf<Boolean>()

    private val repository = FakePresetRepository(
        CustomPreset.of(PresetId(CARD), "Card", listOf(listOf("Bank"))),
        CustomPreset.of(PresetId(SITE), "Site", listOf(emptyList())),
    )

    @Test
    fun `a preset opened in the list opens its tab, shown, titled by its name`() = runTest {
        val state = perform(edit(CARD), edit(SITE))

        assertEquals(listOf(PresetTab.Edit(CARD, "Card"), PresetTab.Edit(SITE, "Site")), state.tabs)
        assertEquals(presetPane(SITE), state.active)
        assertEquals(listOf(true, true), taken)
    }

    @Test
    fun `a preset open already is shown, not opened twice`() = runTest {
        val state = perform(edit(CARD), edit(SITE), edit(CARD))

        assertEquals(listOf(presetPane(CARD), presetPane(SITE)), state.tabs.keys)
        assertEquals(presetPane(CARD), state.active)
    }

    @Test
    fun `each new preset made has a tab of its own`() = runTest {
        val state = perform(create(), create())

        assertEquals(listOf(PresetTab.New(newPresetTab(1)), PresetTab.New(newPresetTab(2))), state.tabs)
        assertEquals(newPresetTab(2), state.active)
    }

    @Test
    fun `back in the list is the app's - back to the settings`() = runTest {
        val state = perform(fromList(MviNavEvent.NavigateBack))

        assertEquals(listOf(false), taken)
        assertEquals(PresetsContainerState(), state)
    }

    @Test
    fun `the rest the list goes to is the app's`() = runTest {
        perform(fromList(MviNavEvent.NavigateTo(SettingsRoute(DB))))

        assertEquals(listOf(false), taken)
    }

    @Test
    fun `back from a form - saved or left - closes its tab, the next one shown`() = runTest {
        val state = perform(
            edit(CARD),
            create(),
            edit(SITE),
            { onSelectTab(newPresetTab(1)) },
            fromTab(newPresetTab(1), MviNavEvent.NavigateBack),
        )

        assertEquals(listOf(presetPane(CARD), presetPane(SITE)), state.tabs.keys)
        assertEquals(presetPane(SITE), state.active)
        assertEquals(true, taken.last())
    }

    @Test
    fun `the rest a form goes to is the app's`() = runTest {
        perform(edit(CARD), fromTab(presetPane(CARD), MviNavEvent.NavigateTo(SettingsRoute(DB))))

        assertEquals(false, taken.last())
    }

    @Test
    fun `opened from the form of an entry - with the tab of the new preset, shown`() = runTest {
        val state = perform(opening = PresetEditRoute())

        assertEquals(listOf(PresetTab.New(newPresetTab(1))), state.tabs)
        assertEquals(newPresetTab(1), state.active)
    }

    @Test
    fun `back from the tab it was opened with is the app's - back to the form of the entry`() = runTest {
        val state = perform(fromTab(newPresetTab(1), MviNavEvent.NavigateBack), opening = PresetEditRoute())

        assertEquals(listOf(false), taken)
        assertEquals(listOf(newPresetTab(1)), state.tabs.keys)
    }

    @Test
    fun `opened with a preset - its tab titled by its name`() = runTest {
        processor(PresetEditRoute(CARD)).testWithInternalState(this) {
            // The titles come from the presets, as while the container is shown
            runOnCreate()

            assertEquals(listOf(PresetTab.Edit(CARD, "Card")), awaitInternalState().tabs)
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `the cross of a tab closes its form - it asks`() = runTest {
        val open = PresetsContainerState(tabs = listOf(PresetTab.Edit(CARD, "Card"), PresetTab.Edit(SITE, "Site")), active = presetPane(SITE))

        processor().testWithInternalState(this, open) {
            containerHost.onCloseTab(presetPane(CARD))

            assertEquals(presetPane(CARD), awaitInternalState().active)
            expectSideEffect(PresetsContainerSideEffect.ClosePane(presetPane(CARD)))
        }
    }

    @Test
    fun `the cross of a tab not open changes nothing`() = runTest {
        val state = perform({ onCloseTab(presetPane(CARD)) })

        assertNull(state.active)
    }

    @Test
    fun `the panes kept are the list and the tabs`() = runTest {
        val state = perform(edit(CARD), create())

        assertEquals(setOf(PRESETS_LIST_PANE, presetPane(CARD), newPresetTab(1)), state.paneKeys)
    }

    @Test
    fun `a preset renamed renames its tab`() = runTest {
        val processor = processor(PresetEditRoute(CARD))

        processor.testWithInternalState(this) {
            // Listening, as while the container is shown
            runOnCreate()
            assertEquals("Card", (awaitInternalState().tabs.single() as PresetTab.Edit).title)

            repository.save(CustomPreset.of(PresetId(CARD), "Card, renamed", listOf(listOf("Bank"))))

            assertEquals("Card, renamed", (awaitInternalState().tabs.single() as PresetTab.Edit).title)
            cancelAndIgnoreRemainingItems()
        }
    }

    private fun edit(presetId: String) = fromList(MviNavEvent.NavigateTo(PresetEditRoute(presetId)))

    private fun create() = fromList(MviNavEvent.NavigateTo(PresetEditRoute()))

    private fun fromList(event: MviNavEvent): PresetsContainerProcessor.() -> Unit =
        { taken += listRouter.route(event) }

    private fun fromTab(key: String, event: MviNavEvent): PresetsContainerProcessor.() -> Unit =
        { taken += tabRouter(key).route(event) }

    /** Runs [actions] one after another on a new processor, opened with [opening]; the state they end in. */
    private suspend fun TestScope.perform(
        vararg actions: PresetsContainerProcessor.() -> Unit,
        opening: PresetEditRoute? = null,
    ): PresetsContainerState {
        val processor = processor(opening)
        processor.testWithInternalState(this) {
            containerHost.runEach(actions)
            cancelAndIgnoreRemainingItems()
        }
        return processor.container.stateFlow.value
    }

    private fun processor(opening: PresetEditRoute? = null) = PresetsContainerProcessor(opening, repository)

    private val List<PresetTab>.keys get() = map { it.key }

    private companion object {
        const val DB = "content://docs/passwords.kdbx"
        const val CARD = "card"
        const val SITE = "site"
    }
}
