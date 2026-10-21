package ru.kino.dev.settings.pres.appearance

import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
 * @param themeMode the chosen mode; null until read - its choice is disabled
 * @param language the chosen language; null until read - its choice is disabled
 * @param passwordColoring whether passwords are colored; null until read - its choice is disabled
 * @param userNameInList whether the list shows user names; null until read - its choice is disabled
 * @param entryCount whether groups show their number; null until read - its choice is disabled
 * @param entryCountNested whether the number counts the groups below; null until read - its choice is disabled
 * @param userNameHidden the open database hides user names: "user name in the list" is off and takes nothing
 * @param message for the snackbar, until it takes it
 */
data class AppearanceState(
    val themeMode: ThemeMode? = null,
    val language: AppLanguage? = null,
    val passwordColoring: Boolean? = null,
    val userNameInList: Boolean? = null,
    val entryCount: Boolean? = null,
    val entryCountNested: Boolean? = null,
    val userNameHidden: Boolean = false,
    val message: AppearanceMessage? = null,
)

/**
 * How the app looks (pass-docs, Step 55, Step 57, Step 58): the mode of the theme, the language, the coloring
 * of passwords, the user names in the list, the number of entries of a group. A choice is shown at once and written in the background - only
 * the store of the app settings, not the database: the screen does not wait for it, nothing is disabled
 * meanwhile (pass-docs, Step 56, item 4). The whole app takes it from the repository; one not written shows
 * what the repository holds, with a snackbar.
 *
 * The database [dbKey] hiding user names (pass-docs, Step 65) keeps them out of its list: "user name in the list" is
 * shown off and takes nothing while it is open; the choice of the app is not written - another database shows it
 * (pass-docs, Step 56, item 5).
 */
@HiltViewModel(assistedFactory = AppearanceProcessor.Factory::class)
class AppearanceProcessor @AssistedInject constructor(
    @Assisted private val dbKey: String,
    private val repository: AppearanceRepository,
    private val entrySettings: EntrySettingsRepository,
    private val navigator: Navigator,
    private val log: ErrorLog,
) : ViewModel(), OrbitContainerHost<AppearanceState, AppearanceState, Nothing> {

    @AssistedFactory
    interface Factory {
        fun create(dbKey: String): AppearanceProcessor
    }

    override val container = orbitContainer<AppearanceState, Nothing>(AppearanceState())

    fun action(action: AppearanceAction): Job = intent {
        when (action) {
            AppearanceAction.Load -> {
                val mode = repository.themeMode.first()
                val language = repository.language.first()
                val passwordColoring = repository.passwordColoring.first()
                val userNameInList = repository.userNameInList.first()
                val entryCount = repository.entryCount.first()
                val entryCountNested = repository.entryCountNested.first()
                val userNameHidden = userNameHidden()
                reduce {
                    state.copy(
                        themeMode = mode,
                        language = language,
                        passwordColoring = passwordColoring,
                        userNameInList = userNameInList,
                        entryCount = entryCount,
                        entryCountNested = entryCountNested,
                        userNameHidden = userNameHidden,
                    )
                }
            }

            AppearanceAction.Back -> navigator.navigate(MviNavEvent.NavigateBack)

            is AppearanceAction.SelectTheme ->
                keep(action.mode, state.themeMode, repository.themeMode, { copy(themeMode = it) }) {
                    repository.save(it)
                }

            is AppearanceAction.SelectLanguage ->
                keep(action.language, state.language, repository.language, { copy(language = it) }) {
                    repository.save(it)
                }

            is AppearanceAction.SetPasswordColoring ->
                keep(action.on, state.passwordColoring, repository.passwordColoring, { copy(passwordColoring = it) }) {
                    repository.savePasswordColoring(it)
                }

            is AppearanceAction.SetUserNameInList -> if (!state.userNameHidden) {
                keep(action.on, state.userNameInList, repository.userNameInList, { copy(userNameInList = it) }) {
                    repository.saveUserNameInList(it)
                }
            }

            is AppearanceAction.SetEntryCount ->
                keep(action.on, state.entryCount, repository.entryCount, { copy(entryCount = it) }) {
                    repository.saveEntryCount(it)
                }

            is AppearanceAction.SetEntryCountNested ->
                keep(action.on, state.entryCountNested, repository.entryCountNested, { copy(entryCountNested = it) }) {
                    repository.saveEntryCountNested(it)
                }

            AppearanceAction.MessageShown -> reduce { state.copy(message = null) }
        }
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
