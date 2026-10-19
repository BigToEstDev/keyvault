package ru.kino.dev.settings.pres.settings

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.Navigator
import javax.inject.Inject

/** No data yet: the cards are fixed, the settings behind them come in the next items (pass-docs, Step 54). */
data object SettingsState

/**
 * The settings - of the app and of the open database. A screen of the open database: its graph is inside
 * `vault`, so the lock takes it away with `vault` (pass-docs, Step 52).
 */
@HiltViewModel
class SettingsProcessor @Inject constructor(
    private val navigator: Navigator,
) : ViewModel(), OrbitContainerHost<SettingsState, SettingsState, Nothing> {

    override val container = orbitContainer<SettingsState, Nothing>(SettingsState)

    fun action(action: SettingsAction): Job = intent {
        when (action) {
            SettingsAction.Back -> navigator.navigate(MviNavEvent.NavigateBack)
        }
    }
}
