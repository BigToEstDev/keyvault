package ru.kino.dev.settings.pres.appearance

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.orbitmvi.orbit.test.testWithInternalState
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.Navigator
import ru.kino.dev.settings.appearance.AppearanceRepository
import ru.kino.dev.settings.appearance.ThemeMode
import java.io.IOException

class AppearanceProcessorTest {

    @Test
    fun `the screen shown reads the chosen mode`() = runTest {
        val repository = FakeAppearanceRepository(ThemeMode.Dark)

        AppearanceProcessor(repository, Navigator()).testWithInternalState(this) {
            containerHost.action(AppearanceAction.Load).join()

            expectInternalState { copy(themeMode = ThemeMode.Dark) }
        }
    }

    @Test
    fun `a mode chosen is kept at once`() = runTest {
        val repository = FakeAppearanceRepository(ThemeMode.System)

        AppearanceProcessor(repository, Navigator()).testWithInternalState(this, AppearanceState(ThemeMode.System)) {
            containerHost.action(AppearanceAction.SelectTheme(ThemeMode.Light)).join()

            expectInternalState { copy(themeMode = ThemeMode.Light, saving = true) }
            expectInternalState { copy(saving = false) }
        }

        assertEquals(listOf(ThemeMode.Light), repository.saved)
    }

    @Test
    fun `a mode not written goes back to the one before, with a message`() = runTest {
        val repository = FakeAppearanceRepository(ThemeMode.System, failSave = true)

        AppearanceProcessor(repository, Navigator()).testWithInternalState(this, AppearanceState(ThemeMode.System)) {
            containerHost.action(AppearanceAction.SelectTheme(ThemeMode.Dark)).join()

            expectInternalState { copy(themeMode = ThemeMode.Dark, saving = true) }
            expectInternalState { copy(themeMode = ThemeMode.System, saving = false, message = AppearanceMessage.NotSaved) }
        }
    }

    @Test
    fun `the mode already chosen is not written again`() = runTest {
        val repository = FakeAppearanceRepository(ThemeMode.Dark)

        AppearanceProcessor(repository, Navigator()).testWithInternalState(this, AppearanceState(ThemeMode.Dark)) {
            containerHost.action(AppearanceAction.SelectTheme(ThemeMode.Dark)).join()
        }

        assertEquals(emptyList<ThemeMode>(), repository.saved)
    }

    @Test
    fun `nothing is chosen before the mode is read`() = runTest {
        val repository = FakeAppearanceRepository(ThemeMode.System)

        AppearanceProcessor(repository, Navigator()).testWithInternalState(this) {
            containerHost.action(AppearanceAction.SelectTheme(ThemeMode.Light)).join()
        }

        assertEquals(emptyList<ThemeMode>(), repository.saved)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `back leaves the screen`() = runTest {
        val navigator = Navigator()
        val events = mutableListOf<MviNavEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { navigator.events.collect { events += it } }

        AppearanceProcessor(FakeAppearanceRepository(ThemeMode.System), navigator).testWithInternalState(this) {
            containerHost.action(AppearanceAction.Back).join()
        }

        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }

    private class FakeAppearanceRepository(
        initial: ThemeMode,
        private val failSave: Boolean = false,
    ) : AppearanceRepository {

        private val mode = MutableStateFlow(initial)

        val saved = mutableListOf<ThemeMode>()

        override val themeMode: Flow<ThemeMode> = mode

        override suspend fun save(mode: ThemeMode) {
            if (failSave) throw IOException("disk full")
            saved += mode
            this.mode.value = mode
        }
    }
}
