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
import ru.kino.dev.settings.appearance.AppLanguage
import ru.kino.dev.settings.appearance.AppearanceRepository
import ru.kino.dev.settings.appearance.ThemeMode
import java.io.IOException

class AppearanceProcessorTest {

    @Test
    fun `the screen shown reads the chosen mode and language`() = runTest {
        val repository = FakeAppearanceRepository(ThemeMode.Dark, AppLanguage.Russian)

        AppearanceProcessor(repository, Navigator()).testWithInternalState(this) {
            containerHost.action(AppearanceAction.Load).join()

            expectInternalState { copy(themeMode = ThemeMode.Dark, language = AppLanguage.Russian) }
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

    @Test
    fun `a language chosen is kept at once`() = runTest {
        val repository = FakeAppearanceRepository(ThemeMode.System)

        AppearanceProcessor(repository, Navigator())
            .testWithInternalState(this, AppearanceState(ThemeMode.System, AppLanguage.System)) {
                containerHost.action(AppearanceAction.SelectLanguage(AppLanguage.English)).join()

                expectInternalState { copy(language = AppLanguage.English, saving = true) }
                expectInternalState { copy(saving = false) }
            }

        assertEquals(listOf(AppLanguage.English), repository.savedLanguages)
    }

    @Test
    fun `a language not written goes back to the one before, with a message`() = runTest {
        val repository = FakeAppearanceRepository(ThemeMode.System, failSave = true)

        AppearanceProcessor(repository, Navigator())
            .testWithInternalState(this, AppearanceState(ThemeMode.System, AppLanguage.System)) {
                containerHost.action(AppearanceAction.SelectLanguage(AppLanguage.Russian)).join()

                expectInternalState { copy(language = AppLanguage.Russian, saving = true) }
                expectInternalState {
                    copy(language = AppLanguage.System, saving = false, message = AppearanceMessage.NotSaved)
                }
            }
    }

    @Test
    fun `the language already chosen is not written again`() = runTest {
        val repository = FakeAppearanceRepository(ThemeMode.System, AppLanguage.Russian)

        AppearanceProcessor(repository, Navigator())
            .testWithInternalState(this, AppearanceState(ThemeMode.System, AppLanguage.Russian)) {
                containerHost.action(AppearanceAction.SelectLanguage(AppLanguage.Russian)).join()
            }

        assertEquals(emptyList<AppLanguage>(), repository.savedLanguages)
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
        initialLanguage: AppLanguage = AppLanguage.System,
        private val failSave: Boolean = false,
    ) : AppearanceRepository {

        private val mode = MutableStateFlow(initial)

        private val chosenLanguage = MutableStateFlow(initialLanguage)

        val saved = mutableListOf<ThemeMode>()

        val savedLanguages = mutableListOf<AppLanguage>()

        override val themeMode: Flow<ThemeMode> = mode

        override val language: Flow<AppLanguage> = chosenLanguage

        override suspend fun save(mode: ThemeMode) {
            if (failSave) throw IOException("disk full")
            saved += mode
            this.mode.value = mode
        }

        override suspend fun save(language: AppLanguage) {
            if (failSave) throw IOException("disk full")
            savedLanguages += language
            chosenLanguage.value = language
        }
    }
}
