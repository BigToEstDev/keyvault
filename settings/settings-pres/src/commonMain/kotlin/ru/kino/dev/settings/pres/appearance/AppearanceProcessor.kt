package ru.kino.dev.settings.pres.appearance

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ru.kino.dev.log.ErrorLog
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.Navigator
import ru.kino.dev.settings.appearance.AppLanguage
import ru.kino.dev.settings.appearance.AppearanceRepository
import ru.kino.dev.settings.appearance.ThemeMode
import ru.kino.dev.settings.pres.common.keepSetting
import ru.kino.dev.vault.entries.EntrySettingsFailure
import ru.kino.dev.vault.entries.EntrySettingsRepository

/**
 * How the app looks (pass-docs, Step 55, Step 57, Step 58, Step 74): the mode of the theme, the language, the
 * coloring of passwords and their strength, the user names in the list, the number of entries of a group. A choice is shown at once and written in the background - only
 * the store of the app settings, not the database: the screen does not wait for it, nothing is disabled
 * meanwhile (pass-docs, Step 56, item 4). The whole app takes it from the repository; one not written shows
 * what the repository holds, with a snackbar.
 *
 * The database [dbKey] hiding user names (pass-docs, Step 65) keeps them out of its list: "user name in the list" is
 * shown off and takes nothing while it is open; the choice of the app is not written - another database shows it
 * (pass-docs, Step 56, item 5).
 */
class AppearanceProcessor(
    private val dbKey: String,
    private val repository: AppearanceRepository,
    private val entrySettings: EntrySettingsRepository,
    private val navigator: Navigator,
    private val log: ErrorLog,
) : ViewModel(), OrbitContainerHost<AppearanceState, AppearanceState, Nothing>, AppearanceActions {

    override val container = orbitContainer<AppearanceState, Nothing>(AppearanceState())

    override fun onLoad() {
        intent {
            val mode = repository.themeMode.first()
            val language = repository.language.first()
            val passwordColoring = repository.passwordColoring.first()
            val passwordStrength = repository.passwordStrength.first()
            val userNameInList = repository.userNameInList.first()
            val entryCount = repository.entryCount.first()
            val entryCountNested = repository.entryCountNested.first()
            val userNameHidden = userNameHidden()
            reduce {
                state.copy(
                    themeMode = mode,
                    language = language,
                    passwordColoring = passwordColoring,
                    passwordStrength = passwordStrength,
                    userNameInList = userNameInList,
                    entryCount = entryCount,
                    entryCountNested = entryCountNested,
                    userNameHidden = userNameHidden,
                )
            }
        }
    }

    override fun onBack() {
        intent { navigator.navigate(MviNavEvent.NavigateBack) }
    }

    override fun onSelectTheme(mode: ThemeMode) {
        intent {
            keep(mode, state.themeMode, repository.themeMode, { copy(themeMode = it) }) {
                repository.save(it)
            }
        }
    }

    override fun onSelectLanguage(language: AppLanguage) {
        intent {
            keep(language, state.language, repository.language, { copy(language = it) }) {
                repository.save(it)
            }
        }
    }

    override fun onSetPasswordColoring(on: Boolean) {
        intent {
            keep(on, state.passwordColoring, repository.passwordColoring, { copy(passwordColoring = it) }) {
                repository.savePasswordColoring(it)
            }
        }
    }

    override fun onSetPasswordStrength(on: Boolean) {
        intent {
            keep(on, state.passwordStrength, repository.passwordStrength, { copy(passwordStrength = it) }) {
                repository.savePasswordStrength(it)
            }
        }
    }

    override fun onSetUserNameInList(on: Boolean) {
        intent {
            if (!state.userNameHidden) {
                keep(on, state.userNameInList, repository.userNameInList, { copy(userNameInList = it) }) {
                    repository.saveUserNameInList(it)
                }
            }
        }
    }

    override fun onSetEntryCount(on: Boolean) {
        intent {
            keep(on, state.entryCount, repository.entryCount, { copy(entryCount = it) }) {
                repository.saveEntryCount(it)
            }
        }
    }

    override fun onSetEntryCountNested(on: Boolean) {
        intent {
            keep(on, state.entryCountNested, repository.entryCountNested, { copy(entryCountNested = it) }) {
                repository.saveEntryCountNested(it)
            }
        }
    }

    override fun onMessageShown() {
        intent { reduce { state.copy(message = null) } }
    }

    /** Whether the open database hides user names; not read - as if not, the cause to the log. */
    private suspend fun userNameHidden(): Boolean = try {
        entrySettings.read(dbKey).protection.userName
    } catch (failure: EntrySettingsFailure) {
        log.unexpected(TAG, failure)
        false
    }

    private suspend fun <T : Any> Syntax<AppearanceState, Nothing>.keep(
        chosen: T,
        current: T?,
        kept: Flow<T>,
        show: AppearanceState.(T) -> AppearanceState,
        save: suspend (T) -> Unit,
    ) = keepSetting(chosen, current, kept, show, { copy(message = AppearanceMessage.NotSaved) }, save)

    private companion object {
        const val TAG = "AppearanceProcessor"
    }
}
