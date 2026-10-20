package ru.kino.dev.settings.pres.appearance

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.Navigator
import ru.kino.dev.settings.appearance.AppLanguage
import ru.kino.dev.settings.appearance.AppearanceRepository
import ru.kino.dev.settings.appearance.ThemeMode
import java.io.IOException
import javax.inject.Inject

/**
 * @param themeMode the chosen mode; null until read - the choices are disabled
 * @param language the chosen language; null until read - the choices are disabled
 * @param passwordColoring whether passwords are colored; null until read - the choices are disabled
 * @param saving a choice is being written - the choices are disabled
 * @param message for the snackbar, until it takes it
 */
data class AppearanceState(
    val themeMode: ThemeMode? = null,
    val language: AppLanguage? = null,
    val passwordColoring: Boolean? = null,
    val saving: Boolean = false,
    val message: AppearanceMessage? = null,
)

/**
 * How the app looks (pass-docs, Step 55, Step 57, Step 58): the mode of the theme, the language, the coloring
 * of passwords. A choice is kept
 * at once - the whole app takes it from the repository; one not written goes back to what it was, with a
 * snackbar.
 */
@HiltViewModel
class AppearanceProcessor @Inject constructor(
    private val repository: AppearanceRepository,
    private val navigator: Navigator,
) : ViewModel(), OrbitContainerHost<AppearanceState, AppearanceState, Nothing> {

    override val container = orbitContainer<AppearanceState, Nothing>(AppearanceState())

    fun action(action: AppearanceAction): Job = intent {
        when (action) {
            AppearanceAction.Load -> {
                val mode = repository.themeMode.first()
                val language = repository.language.first()
                val passwordColoring = repository.passwordColoring.first()
                reduce { state.copy(themeMode = mode, language = language, passwordColoring = passwordColoring) }
            }

            AppearanceAction.Back -> navigator.navigate(MviNavEvent.NavigateBack)

            is AppearanceAction.SelectTheme ->
                keep(action.mode, state.themeMode, { copy(themeMode = it) }) { repository.save(it) }

            is AppearanceAction.SelectLanguage ->
                keep(action.language, state.language, { copy(language = it) }) { repository.save(it) }

            is AppearanceAction.SetPasswordColoring ->
                keep(action.on, state.passwordColoring, { copy(passwordColoring = it) }) {
                    repository.savePasswordColoring(it)
                }

            AppearanceAction.MessageShown -> reduce { state.copy(message = null) }
        }
    }

    /**
     * Shows [chosen] at once and writes it; back to [previous] when it could not be written. Nothing until
     * the setting is read, while another choice is being written, or when it is the same.
     */
    private suspend fun <T : Any> Syntax<AppearanceState, Nothing>.keep(
        chosen: T,
        previous: T?,
        show: AppearanceState.(T) -> AppearanceState,
        save: suspend (T) -> Unit,
    ) {
        if (previous == null || state.saving || chosen == previous) return

        reduce { state.show(chosen).copy(saving = true) }
        try {
            save(chosen)
            reduce { state.copy(saving = false) }
        } catch (_: IOException) {
            reduce { state.show(previous).copy(saving = false, message = AppearanceMessage.NotSaved) }
        }
    }
}
