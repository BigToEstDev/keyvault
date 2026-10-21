package ru.kino.dev.settings.pres.privacy

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.orbitmvi.orbit.test.testWithInternalState
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.Navigator
import ru.kino.dev.settings.privacy.FakePrivacySettingsRepository

class PrivacyProcessorTest {

    @Test
    fun `the screen shown reads what is chosen`() = runTest {
        val repository = FakePrivacySettingsRepository(allowScreenshots = true)

        PrivacyProcessor(repository, Navigator()).testWithInternalState(this) {
            containerHost.action(PrivacyAction.Load).join()

            expectInternalState { copy(allowScreenshots = true) }
        }
    }

    @Test
    fun `screenshots allowed are kept at once`() = runTest {
        val repository = FakePrivacySettingsRepository()

        PrivacyProcessor(repository, Navigator()).testWithInternalState(this, PrivacyState(allowScreenshots = false)) {
            containerHost.action(PrivacyAction.SetAllowScreenshots(true)).join()

            expectInternalState { copy(allowScreenshots = true) }
        }

        assertEquals(listOf(true), repository.savedAllowScreenshots)
    }

    @Test
    fun `screenshots not written show what is kept, with a message`() = runTest {
        val repository = FakePrivacySettingsRepository(failSave = true)

        PrivacyProcessor(repository, Navigator()).testWithInternalState(this, PrivacyState(allowScreenshots = false)) {
            containerHost.action(PrivacyAction.SetAllowScreenshots(true)).join()

            expectInternalState { copy(allowScreenshots = true) }
            expectInternalState { copy(allowScreenshots = false, message = PrivacyMessage.NotSaved) }
        }
    }

    @Test
    fun `nothing is written before the setting is read`() = runTest {
        val repository = FakePrivacySettingsRepository()

        PrivacyProcessor(repository, Navigator()).testWithInternalState(this) {
            containerHost.action(PrivacyAction.SetAllowScreenshots(true)).join()
        }

        assertEquals(emptyList<Boolean>(), repository.savedAllowScreenshots)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `back leaves the screen`() = runTest {
        val navigator = Navigator()
        val events = mutableListOf<MviNavEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { navigator.events.collect { events += it } }

        PrivacyProcessor(FakePrivacySettingsRepository(), navigator).testWithInternalState(this) {
            containerHost.action(PrivacyAction.Back).join()
        }

        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }
}
