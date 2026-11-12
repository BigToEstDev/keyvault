package ru.kino.dev.settings.pres.settings

import androidx.lifecycle.ViewModel
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.Navigator
import ru.kino.dev.settings.pres.navigation.AboutRoute
import ru.kino.dev.settings.pres.navigation.route

/**
 * The settings - of the app and of the open database. A screen of the open database: its graph is inside
 * `vault`, so the lock takes it away with `vault` (pass-docs, Step 52).
 */
class SettingsProcessor(
    private val dbKey: String,
    private val navigator: Navigator,
) : ViewModel(), OrbitContainerHost<SettingsState, SettingsState, Nothing>, SettingsActions {

    override val container = orbitContainer<SettingsState, Nothing>(SettingsState)

    override fun onBack() {
        intent { navigator.navigate(MviNavEvent.NavigateBack) }
    }

    override fun onOpen(item: SettingsItem) {
        intent { navigator.navigate(MviNavEvent.NavigateTo(item.route(dbKey))) }
    }

    override fun onOpenAbout() {
        intent { navigator.navigate(MviNavEvent.NavigateTo(AboutRoute)) }
    }
}
