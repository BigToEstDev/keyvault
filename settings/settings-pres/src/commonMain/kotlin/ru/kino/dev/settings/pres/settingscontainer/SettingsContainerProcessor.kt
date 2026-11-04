package ru.kino.dev.settings.pres.settingscontainer

import androidx.lifecycle.ViewModel
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.pane.PaneRouter
import ru.kino.dev.settings.pres.navigation.route
import ru.kino.dev.settings.pres.settings.SettingsItem

/**
 * The container of the settings of [dbKey] on a wide window (pass-docs, Step 101, `docs/dev/navigation.md`): the main
 * settings screen on the left, the page chosen in it on the right - in one tab: another chosen - the tab opens it
 * instead, its cross - the right empty.
 * The screens in it are those of a phone, each with its own processor; what they navigate to that is the container's
 * comes here through [listRouter] and [pageRouter], the rest goes on to the navigation of the app: "back" of the list -
 * to `vault`, the presets - their own container over this one, the recycle bin from the entries - its container.
 */
class SettingsContainerProcessor(
    private val dbKey: String,
) : ViewModel(), OrbitContainerHost<SettingsContainerState, SettingsContainerState, Nothing>, SettingsContainerActions {

    override val container = orbitContainer<SettingsContainerState, Nothing>(SettingsContainerState())

    override val listRouter = PaneRouter { event ->
        val route = (event as? MviNavEvent.NavigateTo)?.route
        val page = SettingsItem.entries.firstOrNull { it.isContainerPage && it.route(dbKey) == route }
            ?: return@PaneRouter false
        intent { reduce { state.copy(page = page) } }
        true
    }

    /** Only from the page shown: another one by now - nothing changes, and the event is still the container's. */
    override fun pageRouter(item: SettingsItem) = PaneRouter { event ->
        if (event != MviNavEvent.NavigateBack) return@PaneRouter false
        intent { reduce { if (state.page == item) state.copy(page = null) else state } }
        true
    }

    override fun onSelectTab(key: String) = Unit

    override fun onCloseTab(key: String) {
        intent { reduce { if (state.page?.let(::settingsPagePane) == key) state.copy(page = null) else state } }
    }
}
