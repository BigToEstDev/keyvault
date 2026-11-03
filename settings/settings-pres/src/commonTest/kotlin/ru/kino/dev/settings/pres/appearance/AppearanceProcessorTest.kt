package ru.kino.dev.settings.pres.appearance

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.orbitmvi.orbit.test.testWithInternalState
import ru.kino.dev.core.FieldProtection
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.AppNavigator
import ru.kino.dev.settings.appearance.AppLanguage
import ru.kino.dev.settings.appearance.FakeAppearanceRepository
import ru.kino.dev.settings.appearance.ThemeMode
import ru.kino.dev.settings.pres.testing.RecordingLog
import ru.kino.dev.settings.pres.testing.runToEnd
import ru.kino.dev.vault.entries.EntrySettingsFailure
import ru.kino.dev.vault.entries.FakeEntrySettingsRepository

class AppearanceProcessorTest {

    private val entrySettings = FakeEntrySettingsRepository()
    private val log = RecordingLog()

    @Test
    fun `the screen shown reads the chosen mode and language`() = runTest {
        val repository = FakeAppearanceRepository(ThemeMode.Dark, AppLanguage.Russian)

        AppearanceProcessor(DB, repository, entrySettings, AppNavigator(), log).testWithInternalState(this) {
            containerHost.runToEnd { onLoad() }

            expectInternalState {
                copy(
                    themeMode = ThemeMode.Dark,
                    language = AppLanguage.Russian,
                    passwordColoring = true,
                    passwordStrength = false,
                    userNameInList = false,
                    entryCount = true,
                    entryCountNested = true,
                )
            }
        }
    }

    @Test
    fun `a mode chosen is kept at once`() = runTest {
        val repository = FakeAppearanceRepository(ThemeMode.System)

        AppearanceProcessor(DB, repository, entrySettings, AppNavigator(), log).testWithInternalState(this, AppearanceState(ThemeMode.System)) {
            containerHost.runToEnd { onSelectTheme(ThemeMode.Light) }

            expectInternalState { copy(themeMode = ThemeMode.Light) }
        }

        assertEquals(listOf(ThemeMode.Light), repository.saved)
    }

    @Test
    fun `a mode not written goes back to the one before, with a message`() = runTest {
        val repository = FakeAppearanceRepository(ThemeMode.System, failSave = true)

        AppearanceProcessor(DB, repository, entrySettings, AppNavigator(), log).testWithInternalState(this, AppearanceState(ThemeMode.System)) {
            containerHost.runToEnd { onSelectTheme(ThemeMode.Dark) }

            expectInternalState { copy(themeMode = ThemeMode.Dark) }
            expectInternalState { copy(themeMode = ThemeMode.System, message = AppearanceMessage.NotSaved) }
        }
    }

    @Test
    fun `a mode not written shows the one the repository holds, not the one shown before`() = runTest {
        val repository = FakeAppearanceRepository(ThemeMode.Dark, failSave = true)

        AppearanceProcessor(DB, repository, entrySettings, AppNavigator(), log).testWithInternalState(this, AppearanceState(ThemeMode.System)) {
            containerHost.runToEnd { onSelectTheme(ThemeMode.Light) }

            expectInternalState { copy(themeMode = ThemeMode.Light) }
            expectInternalState { copy(themeMode = ThemeMode.Dark, message = AppearanceMessage.NotSaved) }
        }
    }

    @Test
    fun `the mode already chosen is not written again`() = runTest {
        val repository = FakeAppearanceRepository(ThemeMode.Dark)

        AppearanceProcessor(DB, repository, entrySettings, AppNavigator(), log).testWithInternalState(this, AppearanceState(ThemeMode.Dark)) {
            containerHost.runToEnd { onSelectTheme(ThemeMode.Dark) }
        }

        assertEquals(emptyList<ThemeMode>(), repository.saved)
    }

    @Test
    fun `nothing is chosen before the mode is read`() = runTest {
        val repository = FakeAppearanceRepository(ThemeMode.System)

        AppearanceProcessor(DB, repository, entrySettings, AppNavigator(), log).testWithInternalState(this) {
            containerHost.runToEnd { onSelectTheme(ThemeMode.Light) }
        }

        assertEquals(emptyList<ThemeMode>(), repository.saved)
    }

    @Test
    fun `a language chosen is kept at once`() = runTest {
        val repository = FakeAppearanceRepository(ThemeMode.System)

        AppearanceProcessor(DB, repository, entrySettings, AppNavigator(), log)
            .testWithInternalState(this, AppearanceState(ThemeMode.System, AppLanguage.System)) {
                containerHost.runToEnd { onSelectLanguage(AppLanguage.English) }

                expectInternalState { copy(language = AppLanguage.English) }
            }

        assertEquals(listOf(AppLanguage.English), repository.savedLanguages)
    }

    @Test
    fun `a language not written goes back to the one before, with a message`() = runTest {
        val repository = FakeAppearanceRepository(ThemeMode.System, failSave = true)

        AppearanceProcessor(DB, repository, entrySettings, AppNavigator(), log)
            .testWithInternalState(this, AppearanceState(ThemeMode.System, AppLanguage.System)) {
                containerHost.runToEnd { onSelectLanguage(AppLanguage.Russian) }

                expectInternalState { copy(language = AppLanguage.Russian) }
                expectInternalState {
                    copy(language = AppLanguage.System, message = AppearanceMessage.NotSaved)
                }
            }
    }

    @Test
    fun `the language already chosen is not written again`() = runTest {
        val repository = FakeAppearanceRepository(ThemeMode.System, AppLanguage.Russian)

        AppearanceProcessor(DB, repository, entrySettings, AppNavigator(), log)
            .testWithInternalState(this, AppearanceState(ThemeMode.System, AppLanguage.Russian)) {
                containerHost.runToEnd { onSelectLanguage(AppLanguage.Russian) }
            }

        assertEquals(emptyList<AppLanguage>(), repository.savedLanguages)
    }

    @Test
    fun `password coloring switched is kept at once`() = runTest {
        val repository = FakeAppearanceRepository(ThemeMode.System)

        AppearanceProcessor(DB, repository, entrySettings, AppNavigator(), log)
            .testWithInternalState(this, AppearanceState(ThemeMode.System, AppLanguage.System, passwordColoring = true)) {
                containerHost.runToEnd { onSetPasswordColoring(false) }

                expectInternalState { copy(passwordColoring = false) }
            }

        assertEquals(listOf(false), repository.savedColoring)
    }

    @Test
    fun `password coloring not written goes back, with a message`() = runTest {
        val repository = FakeAppearanceRepository(ThemeMode.System, failSave = true)

        AppearanceProcessor(DB, repository, entrySettings, AppNavigator(), log)
            .testWithInternalState(this, AppearanceState(ThemeMode.System, AppLanguage.System, passwordColoring = true)) {
                containerHost.runToEnd { onSetPasswordColoring(false) }

                expectInternalState { copy(passwordColoring = false) }
                expectInternalState { copy(passwordColoring = true, message = AppearanceMessage.NotSaved) }
            }
    }

    @Test
    fun `password strength switched is kept at once`() = runTest {
        val repository = FakeAppearanceRepository(ThemeMode.System)

        AppearanceProcessor(DB, repository, entrySettings, AppNavigator(), log)
            .testWithInternalState(this, AppearanceState(ThemeMode.System, AppLanguage.System, passwordStrength = false)) {
                containerHost.runToEnd { onSetPasswordStrength(true) }

                expectInternalState { copy(passwordStrength = true) }
            }

        assertEquals(listOf(true), repository.savedStrength)
    }

    @Test
    fun `password strength not written goes back, with a message`() = runTest {
        val repository = FakeAppearanceRepository(ThemeMode.System, failSave = true)

        AppearanceProcessor(DB, repository, entrySettings, AppNavigator(), log)
            .testWithInternalState(this, AppearanceState(ThemeMode.System, AppLanguage.System, passwordStrength = false)) {
                containerHost.runToEnd { onSetPasswordStrength(true) }

                expectInternalState { copy(passwordStrength = true) }
                expectInternalState { copy(passwordStrength = false, message = AppearanceMessage.NotSaved) }
            }
    }

    @Test
    fun `user names in the list switched are kept at once`() = runTest {
        val repository = FakeAppearanceRepository(ThemeMode.System)

        AppearanceProcessor(DB, repository, entrySettings, AppNavigator(), log)
            .testWithInternalState(this, AppearanceState(ThemeMode.System, AppLanguage.System, userNameInList = false)) {
                containerHost.runToEnd { onSetUserNameInList(true) }

                expectInternalState { copy(userNameInList = true) }
            }

        assertEquals(listOf(true), repository.savedUserNameInList)
    }

    @Test
    fun `user names in the list not written go back, with a message`() = runTest {
        val repository = FakeAppearanceRepository(ThemeMode.System, failSave = true)

        AppearanceProcessor(DB, repository, entrySettings, AppNavigator(), log)
            .testWithInternalState(this, AppearanceState(ThemeMode.System, AppLanguage.System, userNameInList = false)) {
                containerHost.runToEnd { onSetUserNameInList(true) }

                expectInternalState { copy(userNameInList = true) }
                expectInternalState { copy(userNameInList = false, message = AppearanceMessage.NotSaved) }
            }
    }

    @Test
    fun `the number of entries switched is kept at once`() = runTest {
        val repository = FakeAppearanceRepository(ThemeMode.System)

        AppearanceProcessor(DB, repository, entrySettings, AppNavigator(), log)
            .testWithInternalState(this, AppearanceState(ThemeMode.System, AppLanguage.System, entryCount = true)) {
                containerHost.runToEnd { onSetEntryCount(false) }

                expectInternalState { copy(entryCount = false) }
            }

        assertEquals(listOf(false), repository.savedEntryCount)
    }

    @Test
    fun `the number of entries not written shows what is kept, with a message`() = runTest {
        val repository = FakeAppearanceRepository(ThemeMode.System, failSave = true)

        AppearanceProcessor(DB, repository, entrySettings, AppNavigator(), log)
            .testWithInternalState(this, AppearanceState(ThemeMode.System, AppLanguage.System, entryCount = true)) {
                containerHost.runToEnd { onSetEntryCount(false) }

                expectInternalState { copy(entryCount = false) }
                expectInternalState { copy(entryCount = true, message = AppearanceMessage.NotSaved) }
            }
    }

    @Test
    fun `counting the nested groups switched is kept at once`() = runTest {
        val repository = FakeAppearanceRepository(ThemeMode.System)

        AppearanceProcessor(DB, repository, entrySettings, AppNavigator(), log)
            .testWithInternalState(this, AppearanceState(ThemeMode.System, AppLanguage.System, entryCountNested = true)) {
                containerHost.runToEnd { onSetEntryCountNested(false) }

                expectInternalState { copy(entryCountNested = false) }
            }

        assertEquals(listOf(false), repository.savedEntryCountNested)
    }

    @Test
    fun `counting the nested groups not written shows what is kept, with a message`() = runTest {
        val repository = FakeAppearanceRepository(ThemeMode.System, failSave = true)

        AppearanceProcessor(DB, repository, entrySettings, AppNavigator(), log)
            .testWithInternalState(this, AppearanceState(ThemeMode.System, AppLanguage.System, entryCountNested = true)) {
                containerHost.runToEnd { onSetEntryCountNested(false) }

                expectInternalState { copy(entryCountNested = false) }
                expectInternalState { copy(entryCountNested = true, message = AppearanceMessage.NotSaved) }
            }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `back leaves the screen`() = runTest {
        val navigator = AppNavigator()
        val events = mutableListOf<MviNavEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { navigator.events.collect { events += it } }

        AppearanceProcessor(DB, FakeAppearanceRepository(ThemeMode.System), entrySettings, navigator, log).testWithInternalState(this) {
            containerHost.runToEnd { onBack() }
        }

        assertEquals(listOf<MviNavEvent>(MviNavEvent.NavigateBack), events)
    }

    @Test
    fun `a database hiding user names shows them off, the choice of the app untouched`() = runTest {
        entrySettings.entries = entrySettings.entries.copy(protection = FieldProtection.DEFAULT.copy(userName = true))
        val repository = FakeAppearanceRepository(initialUserNameInList = true)
        val processor = AppearanceProcessor(DB, repository, entrySettings, AppNavigator(), log)

        processor.testWithInternalState(this) {
            containerHost.runToEnd { onLoad() }
            containerHost.runToEnd { onSetUserNameInList(false) }
            cancelAndIgnoreRemainingItems()
        }

        assertEquals(true, processor.container.stateFlow.value.userNameHidden)
        assertEquals(emptyList<Boolean>(), repository.savedUserNameInList)
    }

    @Test
    fun `the protection not read - the user names are as the app chose, the cause to the log`() = runTest {
        entrySettings.failure = EntrySettingsFailure.Unexpected()
        val processor = AppearanceProcessor(DB, FakeAppearanceRepository(), entrySettings, AppNavigator(), log)

        processor.testWithInternalState(this) {
            containerHost.runToEnd { onLoad() }
            cancelAndIgnoreRemainingItems()
        }

        assertEquals(false, processor.container.stateFlow.value.userNameHidden)
        assertEquals(1, log.logged.size)
    }

    private companion object {
        const val DB = "content://docs/passwords.kdbx"
    }
}
