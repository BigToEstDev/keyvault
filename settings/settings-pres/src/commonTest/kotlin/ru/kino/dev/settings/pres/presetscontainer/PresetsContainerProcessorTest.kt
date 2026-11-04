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
import ru.kino.dev.settings.pres.navigation.PresetReadRoute
import ru.kino.dev.settings.pres.testing.runEach

class PresetsContainerProcessorTest {

    /** What the routers answered, in order: true - taken by the container. */
    private val taken = mutableListOf<Boolean>()

    private val repository = FakePresetRepository(
        CustomPreset.of(PresetId(CARD), "Card", listOf(listOf("Bank"))),
        CustomPreset.of(PresetId(SITE), "Site", listOf(emptyList())),
    )

    @Test
    fun `a preset opened in the list opens its tab, read, shown, titled by its name`() = runTest {
        val state = perform(read(CARD), read(SITE))

        assertEquals(listOf(PresetTab.Preset(CARD, "Card"), PresetTab.Preset(SITE, "Site")), state.tabs)
        assertEquals(presetPane(SITE), state.active)
        assertEquals(listOf(true, true), taken)
    }

    @Test
    fun `the standard preset opens its tab, read`() = runTest {
        val state = perform(read(null))

        assertEquals(listOf(PresetTab.Preset(null)), state.tabs)
        assertEquals(presetPane(null), state.active)
    }

    @Test
    fun `a preset open already is shown, not opened twice`() = runTest {
        val state = perform(read(CARD), read(SITE), read(CARD))

        assertEquals(listOf(presetPane(CARD), presetPane(SITE)), state.tabs.keys)
        assertEquals(presetPane(CARD), state.active)
    }

    @Test
    fun `edit in the menu of the list opens the form in the tab of the preset`() = runTest {
        val state = perform(edit(CARD))

        assertEquals(listOf(PresetTab.Preset(CARD, "Card", editing = true)), state.tabs)
        assertEquals(presetPane(CARD), state.active)
    }

    @Test
    fun `edit in the menu of the list of a preset read - its form over the reading`() = runTest {
        val state = perform(read(CARD), read(SITE), edit(CARD))

        assertEquals(PresetTab.Preset(CARD, "Card", editing = true), state.tabs.first())
        assertEquals(presetPane(CARD), state.active)
    }

    @Test
    fun `each new preset made has a tab of its own`() = runTest {
        val state = perform(create(), create())

        assertEquals(listOf(PresetTab.New(newPresetTab(1), true), PresetTab.New(newPresetTab(2), true)), state.tabs)
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
    fun `edit from the reading - the form over it`() = runTest {
        val state = perform(read(CARD), fromReading(CARD, MviNavEvent.NavigateTo(PresetEditRoute(CARD))))

        assertEquals(PresetTab.Preset(CARD, "Card", editing = true), state.tabs.single())
        assertEquals(presetFormPane(CARD), state.shown?.pane)
        assertEquals(true, taken.last())
    }

    @Test
    fun `back from the form - saved or left - the reading again`() = runTest {
        val state = perform(edit(CARD), fromForm(CARD, MviNavEvent.NavigateBack))

        assertEquals(PresetTab.Preset(CARD, "Card"), state.tabs.single())
        assertEquals(true, taken.last())
    }

    @Test
    fun `back from the reading - deleted or gone - closes its tab, the next one shown`() = runTest {
        val state = perform(
            read(CARD),
            read(SITE),
            create(),
            { onSelectTab(presetPane(SITE)) },
            fromReading(SITE, MviNavEvent.NavigateBack),
        )

        assertEquals(listOf(presetPane(CARD), newPresetTab(1)), state.tabs.keys)
        assertEquals(newPresetTab(1), state.active)
        assertEquals(true, taken.last())
    }

    @Test
    fun `past the reading from the form - deleted or closed - closes its tab`() = runTest {
        val past = MviNavEvent.NavigateBackTo(PresetReadRoute(CARD), inclusive = true, orBack = true)

        val state = perform(read(CARD), edit(CARD), fromForm(CARD, past))

        assertEquals(emptyList(), state.tabs)
        assertNull(state.active)
    }

    @Test
    fun `a page under the one shown changes nothing - still the container's`() = runTest {
        val state = perform(edit(CARD), fromReading(CARD, MviNavEvent.NavigateBack))

        assertEquals(PresetTab.Preset(CARD, "Card", editing = true), state.tabs.single())
        assertEquals(true, taken.last())
    }

    @Test
    fun `the rest a page goes to is the app's`() = runTest {
        perform(read(CARD), fromReading(CARD, MviNavEvent.NavigateTo(SettingsRoute(DB))))

        assertEquals(false, taken.last())
    }

    @Test
    fun `a new preset saved from the list is read in its tab`() = runTest {
        val made = MviNavEvent.NavigateTo(PresetReadRoute(SITE), popUpTo = PresetEditRoute(readSaved = true), inclusive = true)

        val state = perform(create(), fromNew(newPresetTab(1), made))

        assertEquals(listOf(PresetTab.Preset(SITE, "Site")), state.tabs)
        assertEquals(presetPane(SITE), state.active)
        assertEquals(true, taken.last())
    }

    @Test
    fun `back from a new preset - left - closes its tab`() = runTest {
        val state = perform(read(CARD), create(), fromNew(newPresetTab(1), MviNavEvent.NavigateBack))

        assertEquals(listOf(presetPane(CARD)), state.tabs.keys)
        assertEquals(presetPane(CARD), state.active)
    }

    @Test
    fun `opened from the form of an entry - with the tab of the new preset, shown`() = runTest {
        val state = perform(opening = PresetEditRoute())

        assertEquals(listOf(PresetTab.New(newPresetTab(1))), state.tabs)
        assertEquals(newPresetTab(1), state.active)
    }

    @Test
    fun `back from the tab it was opened with is the app's - back to the form of the entry`() = runTest {
        val state = perform(fromNew(newPresetTab(1), MviNavEvent.NavigateBack), opening = PresetEditRoute())

        assertEquals(listOf(false), taken)
        assertEquals(listOf(newPresetTab(1)), state.tabs.keys)
    }

    @Test
    fun `the cross of a form asks it - its tab shown`() = runTest {
        val open = PresetsContainerState(
            tabs = listOf(PresetTab.Preset(CARD, "Card", editing = true), PresetTab.Preset(SITE, "Site")),
            active = presetPane(SITE),
        )

        processor().testWithInternalState(this, open) {
            containerHost.onCloseTab(presetPane(CARD))

            assertEquals(presetPane(CARD), awaitInternalState().active)
            expectSideEffect(PresetsContainerSideEffect.ClosePane(presetFormPane(CARD)))
        }
    }

    @Test
    fun `the cross of a new preset asks its form`() = runTest {
        val open = PresetsContainerState(tabs = listOf(PresetTab.New(newPresetTab(1))), active = newPresetTab(1))

        processor().testWithInternalState(this, open) {
            containerHost.onCloseTab(newPresetTab(1))

            expectSideEffect(PresetsContainerSideEffect.ClosePane(newPresetTab(1)))
        }
    }

    @Test
    fun `the cross of a reading closes its tab at once`() = runTest {
        val state = perform(read(CARD), read(SITE), { onCloseTab(presetPane(SITE)) })

        assertEquals(listOf(presetPane(CARD)), state.tabs.keys)
        assertEquals(presetPane(CARD), state.active)
    }

    @Test
    fun `the cross of a tab not open changes nothing`() = runTest {
        val state = perform({ onCloseTab(presetPane(CARD)) })

        assertNull(state.active)
    }

    @Test
    fun `the panes kept are the list and the tabs, the reading under a form`() = runTest {
        val state = perform(read(SITE), edit(CARD), create())

        assertEquals(
            setOf(PRESETS_LIST_PANE, presetPane(SITE), presetPane(CARD), presetFormPane(CARD), newPresetTab(1)),
            state.paneKeys,
        )
    }

    @Test
    fun `the new presets open are counted, the one it was opened with too`() = runTest {
        val state = perform(create(), read(CARD), create(), opening = PresetEditRoute())

        assertEquals(3, state.drafts)
    }

    @Test
    fun `a new preset closed frees its place`() = runTest {
        val state = perform(create(), create(), fromNew(newPresetTab(1), MviNavEvent.NavigateBack))

        assertEquals(1, state.drafts)
    }

    @Test
    fun `a preset renamed renames its tab`() = runTest {
        val open = PresetsContainerState(tabs = listOf(PresetTab.Preset(CARD, "Card")), active = presetPane(CARD))

        processor().testWithInternalState(this, open) {
            // Listening, as while the container is shown
            runOnCreate()
            repository.save(CustomPreset.of(PresetId(CARD), "Card, renamed", listOf(listOf("Bank"))))

            assertEquals("Card, renamed", (awaitInternalState().tabs.single() as PresetTab.Preset).title)
            cancelAndIgnoreRemainingItems()
        }
    }

    private fun read(presetId: String?) = fromList(MviNavEvent.NavigateTo(PresetReadRoute(presetId)))

    private fun edit(presetId: String) = fromList(MviNavEvent.NavigateTo(PresetEditRoute(presetId)))

    private fun create() = fromList(MviNavEvent.NavigateTo(PresetEditRoute(readSaved = true)))

    private fun fromList(event: MviNavEvent): PresetsContainerProcessor.() -> Unit =
        { taken += listRouter.route(event) }

    private fun fromReading(presetId: String?, event: MviNavEvent): PresetsContainerProcessor.() -> Unit =
        { taken += presetRouter(presetId, editing = false).route(event) }

    private fun fromForm(presetId: String?, event: MviNavEvent): PresetsContainerProcessor.() -> Unit =
        { taken += presetRouter(presetId, editing = true).route(event) }

    private fun fromNew(key: String, event: MviNavEvent): PresetsContainerProcessor.() -> Unit =
        { taken += newRouter(key).route(event) }

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
