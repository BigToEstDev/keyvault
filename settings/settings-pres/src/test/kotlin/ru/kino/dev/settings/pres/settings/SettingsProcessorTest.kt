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

class SettingsProcessorTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `back leaves the settings`() = runTest {
        val navigator = Navigator()
        val events = mutableListOf<MviNavEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { navigator.events.collect { events += it } }

        SettingsProcessor(navigator).testWithInternalState(this) {
            containerHost.action(SettingsAction.Back).join()
        }

        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }
}
