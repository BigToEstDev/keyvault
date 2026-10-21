package ru.kino.dev.settings.pres.settings

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.orbitmvi.orbit.test.testWithInternalState
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.Navigator
import ru.kino.dev.settings.pres.navigation.AppearanceRoute

private const val DB = "content://docs/passwords.kdbx"

class SettingsProcessorTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `back leaves the settings`() = runTest {
        val navigator = Navigator()
        val events = mutableListOf<MviNavEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { navigator.events.collect { events += it } }

        SettingsProcessor(DB, navigator).testWithInternalState(this) {
            containerHost.action(SettingsAction.Back).join()
        }

        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `a card with a screen behind it opens the screen`() = runTest {
        val navigator = Navigator()
        val events = mutableListOf<MviNavEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { navigator.events.collect { events += it } }

        SettingsProcessor(DB, navigator).testWithInternalState(this) {
            containerHost.action(SettingsAction.Open(SettingsItem.Appearance)).join()
        }

        assertEquals(listOf(MviNavEvent.NavigateTo(AppearanceRoute(DB))), events)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `a card with no screen made yet opens nothing`() = runTest {
        val navigator = Navigator()
        val events = mutableListOf<MviNavEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { navigator.events.collect { events += it } }

        SettingsProcessor(DB, navigator).testWithInternalState(this) {
            containerHost.action(SettingsAction.Open(SettingsItem.Security)).join()
        }

        assertEquals(emptyList<MviNavEvent>(), events)
    }
}
