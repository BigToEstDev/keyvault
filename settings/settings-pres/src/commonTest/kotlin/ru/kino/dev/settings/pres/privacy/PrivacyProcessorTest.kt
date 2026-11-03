package ru.kino.dev.settings.pres.privacy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.orbitmvi.orbit.test.testWithInternalState
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.AppNavigator
import ru.kino.dev.settings.pres.testing.runToEnd
import ru.kino.dev.settings.privacy.FakePrivacySettingsRepository

class PrivacyProcessorTest {

    @Test
    fun `the screen shown reads what is chosen`() = runTest {
        val repository = FakePrivacySettingsRepository(allowScreenshots = true)

        PrivacyProcessor(repository, AppNavigator()).testWithInternalState(this) {
            containerHost.runToEnd { onLoad() }

            expectInternalState { copy(allowScreenshots = true) }
        }
    }

    @Test
    fun `screenshots allowed are kept at once`() = runTest {
        val repository = FakePrivacySettingsRepository()

        PrivacyProcessor(repository, AppNavigator()).testWithInternalState(this, PrivacyState(allowScreenshots = false)) {
            containerHost.runToEnd { onSetAllowScreenshots(true) }

            expectInternalState { copy(allowScreenshots = true) }
        }

        assertEquals(listOf(true), repository.savedAllowScreenshots)
    }

    @Test
    fun `screenshots not written show what is kept, with a message`() = runTest {
        val repository = FakePrivacySettingsRepository(failSave = true)

        PrivacyProcessor(repository, AppNavigator()).testWithInternalState(this, PrivacyState(allowScreenshots = false)) {
            containerHost.runToEnd { onSetAllowScreenshots(true) }

            expectInternalState { copy(allowScreenshots = true) }
            expectInternalState { copy(allowScreenshots = false, message = PrivacyMessage.NotSaved) }
        }
    }

    @Test
    fun `nothing is written before the setting is read`() = runTest {
        val repository = FakePrivacySettingsRepository()

        PrivacyProcessor(repository, AppNavigator()).testWithInternalState(this) {
            containerHost.runToEnd { onSetAllowScreenshots(true) }
        }

        assertEquals(emptyList<Boolean>(), repository.savedAllowScreenshots)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `back leaves the screen`() = runTest {
        val navigator = AppNavigator()
        val events = mutableListOf<MviNavEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { navigator.events.collect { events += it } }

        PrivacyProcessor(FakePrivacySettingsRepository(), navigator).testWithInternalState(this) {
            containerHost.runToEnd { onBack() }
        }

        assertEquals(listOf<MviNavEvent>(MviNavEvent.NavigateBack), events)
    }
}
