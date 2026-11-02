package ru.kino.dev.settings.pres.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.orbitmvi.orbit.test.testWithInternalState
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.Navigator
import ru.kino.dev.settings.pres.navigation.AppearanceRoute
import ru.kino.dev.settings.pres.navigation.RecycleBinRoute
import ru.kino.dev.settings.pres.testing.runToEnd

private const val DB = "content://docs/passwords.kdbx"

class SettingsProcessorTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `back leaves the settings`() = runTest {
        val navigator = Navigator()
        val events = mutableListOf<MviNavEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { navigator.events.collect { events += it } }

        SettingsProcessor(DB, navigator).testWithInternalState(this) {
            containerHost.runToEnd { onBack() }
        }

        assertEquals(listOf<MviNavEvent>(MviNavEvent.NavigateBack), events)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `a card with a screen behind it opens the screen`() = runTest {
        val navigator = Navigator()
        val events = mutableListOf<MviNavEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { navigator.events.collect { events += it } }

        SettingsProcessor(DB, navigator).testWithInternalState(this) {
            containerHost.runToEnd { onOpen(SettingsItem.Appearance) }
        }

        assertEquals(listOf<MviNavEvent>(MviNavEvent.NavigateTo(AppearanceRoute(DB))), events)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `the card of the recycle bin opens its screen`() = runTest {
        val navigator = Navigator()
        val events = mutableListOf<MviNavEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { navigator.events.collect { events += it } }

        SettingsProcessor(DB, navigator).testWithInternalState(this) {
            containerHost.runToEnd { onOpen(SettingsItem.RecycleBin) }
        }

        assertEquals(listOf<MviNavEvent>(MviNavEvent.NavigateTo(RecycleBinRoute(DB))), events)
    }
}
