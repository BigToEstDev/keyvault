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
import ru.kino.dev.settings.appearance.AppearanceRepository
import ru.kino.dev.settings.appearance.ThemeMode
import java.io.IOException
import javax.inject.Inject

/**
 * @param themeMode the chosen mode; null until read - the choice is disabled
 * @param saving a choice is being written - the choice is disabled
 * @param message for the snackbar, until it takes it
 */
data class AppearanceState(
    val themeMode: ThemeMode? = null,
    val saving: Boolean = false,
    val message: AppearanceMessage? = null,
)

/**
 * How the app looks (pass-docs, Step 55): the mode of the theme for now. A choice is kept at once - the
 * whole app takes it from the repository; one not written goes back to what it was, with a snackbar.
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
                reduce { state.copy(themeMode = mode) }
            }

            AppearanceAction.Back -> navigator.navigate(MviNavEvent.NavigateBack)

            is AppearanceAction.SelectTheme -> select(action.mode)

            AppearanceAction.MessageShown -> reduce { state.copy(message = null) }
        }
    }

    private suspend fun Syntax<AppearanceState, Nothing>.select(mode: ThemeMode) {
        val previous = state.themeMode ?: return
        if (state.saving || mode == previous) return

        reduce { state.copy(themeMode = mode, saving = true) }
        try {
            repository.save(mode)
            reduce { state.copy(saving = false) }
        } catch (_: IOException) {
            reduce { state.copy(themeMode = previous, saving = false, message = AppearanceMessage.NotSaved) }
        }
    }
}
