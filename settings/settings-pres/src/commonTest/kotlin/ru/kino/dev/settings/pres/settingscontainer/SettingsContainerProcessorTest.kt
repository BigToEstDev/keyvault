package ru.kino.dev.settings.pres.settingscontainer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.orbitmvi.orbit.test.testWithInternalState
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.TrashRoute
import ru.kino.dev.settings.pres.navigation.route
import ru.kino.dev.settings.pres.settings.SettingsItem
import ru.kino.dev.settings.pres.testing.runEach

class SettingsContainerProcessorTest {

    /** What the routers answered, in order: true - taken by the container. */
    private val taken = mutableListOf<Boolean>()

    @Test
    fun `nothing chosen - no page, the right is empty`() = runTest {
        val state = perform()

        assertEquals(SettingsContainerState(), state)
        assertEquals(setOf(SETTINGS_LIST_PANE), state.paneKeys)
    }

    @Test
    fun `each page chosen in the list opens on the right`() = runTest {
        val pages = SettingsItem.entries.filter { it.isContainerPage }
        val state = perform(*pages.map(::open).toTypedArray())

        assertEquals(pages.last(), state.page)
        assertEquals(pages.map { true }, taken)
    }

    @Test
    fun `another page chosen takes the place - the one left is not kept`() = runTest {
        val state = perform(open(SettingsItem.Lock), open(SettingsItem.Security))

        assertEquals(SettingsContainerState(SettingsItem.Security), state)
        assertEquals(setOf(SETTINGS_LIST_PANE, settingsPagePane(SettingsItem.Security)), state.paneKeys)
    }

    @Test
    fun `the presets are the app's - their own container, over the settings`() = runTest {
        val state = perform(open(SettingsItem.Presets))

        assertEquals(listOf(false), taken)
        assertEquals(SettingsContainerState(), state)
    }

    @Test
    fun `back in the list is the app's - back to vault`() = runTest {
        val state = perform(open(SettingsItem.Lock), fromList(MviNavEvent.NavigateBack))

        assertEquals(listOf(true, false), taken)
        assertEquals(SettingsContainerState(SettingsItem.Lock), state)
    }

    @Test
    fun `back on the page closes it - the right is empty`() = runTest {
        val state = perform(open(SettingsItem.File), fromPage(SettingsItem.File, MviNavEvent.NavigateBack))

        assertEquals(listOf(true, true), taken)
        assertEquals(SettingsContainerState(), state)
    }

    @Test
    fun `back late from a page left changes nothing`() = runTest {
        val state = perform(
            open(SettingsItem.File),
            open(SettingsItem.Lock),
            fromPage(SettingsItem.File, MviNavEvent.NavigateBack),
        )

        assertEquals(SettingsContainerState(SettingsItem.Lock), state)
    }

    @Test
    fun `the recycle bin from the entries is the app's - its container`() = runTest {
        val state = perform(
            open(SettingsItem.Entries),
            fromPage(SettingsItem.Entries, MviNavEvent.NavigateTo(TrashRoute(DB))),
        )

        assertEquals(listOf(true, false), taken)
        assertEquals(SettingsContainerState(SettingsItem.Entries), state)
    }

    private fun open(item: SettingsItem) = fromList(MviNavEvent.NavigateTo(item.route(DB)))

    private fun fromList(event: MviNavEvent): SettingsContainerProcessor.() -> Unit =
        { taken += listRouter.route(event) }

    private fun fromPage(item: SettingsItem, event: MviNavEvent): SettingsContainerProcessor.() -> Unit =
        { taken += pageRouter(item).route(event) }

    /** Runs [actions] one after another on a new processor; the state they end in. */
    private suspend fun TestScope.perform(vararg actions: SettingsContainerProcessor.() -> Unit): SettingsContainerState {
        val processor = SettingsContainerProcessor(DB)
        processor.testWithInternalState(this) {
            containerHost.runEach(actions)
            cancelAndIgnoreRemainingItems()
        }
        return processor.container.stateFlow.value
    }

    private companion object {
        const val DB = "content://docs/passwords.kdbx"
    }
}
