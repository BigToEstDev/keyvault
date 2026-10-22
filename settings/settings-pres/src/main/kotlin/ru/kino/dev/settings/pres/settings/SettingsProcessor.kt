package ru.kino.dev.settings.pres.settings

import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.Navigator
import ru.kino.dev.settings.pres.navigation.route

/** No data: the cards are fixed, the screens behind them come one by one (pass-docs, Step 55). */
data object SettingsState

/**
 * The settings - of the app and of the open database. A screen of the open database: its graph is inside
 * `vault`, so the lock takes it away with `vault` (pass-docs, Step 52).
 */
@HiltViewModel(assistedFactory = SettingsProcessor.Factory::class)
class SettingsProcessor @AssistedInject constructor(
    @Assisted private val dbKey: String,
    private val navigator: Navigator,
) : ViewModel(), OrbitContainerHost<SettingsState, SettingsState, Nothing> {

    @AssistedFactory
    interface Factory {
        fun create(dbKey: String): SettingsProcessor
    }

    override val container = orbitContainer<SettingsState, Nothing>(SettingsState)

    fun action(action: SettingsAction): Job = intent {
        when (action) {
            SettingsAction.Back -> navigator.navigate(MviNavEvent.NavigateBack)

            is SettingsAction.Open -> navigator.navigate(MviNavEvent.NavigateTo(action.item.route(dbKey)))
        }
    }
}
