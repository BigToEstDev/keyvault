package ru.kino.dev.vault.pres.container

import androidx.lifecycle.ViewModel
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.VaultRootRoute
import ru.kino.dev.pane.PaneRouter
import ru.kino.dev.vault.pres.navigation.EntryReadRoute
import ru.kino.dev.vault.pres.navigation.GroupRoute

/**
 * The container of `vault` on a wide window: the list of a group on the left, the entries open on the right as tabs
 * (pass-docs, Step 98, `docs/dev/navigation.md`). The screens in it are those of a phone, each with its own
 * processor; what they navigate to that is the container's - another group, an entry, back - comes here through
 * [listRouter] and [entryRouter], the rest goes on to the navigation of the app.
 *
 * A router answers at once, from the kind of the event; the state changes after, in an intent.
 */
class VaultContainerProcessor : ViewModel(), OrbitContainerHost<VaultContainerState, VaultContainerState, Nothing>, VaultContainerActions {

    override val container = orbitContainer<VaultContainerState, Nothing>(VaultContainerState())

    /** The navigation of the list on the left. */
    val listRouter = PaneRouter { event ->
        when {
            event is MviNavEvent.NavigateTo && event.route is GroupRoute -> {
                intent { reduce { state.copy(groups = state.groups + (event.route as GroupRoute).groupId) } }
                true
            }

            event is MviNavEvent.NavigateTo && event.route is EntryReadRoute -> {
                intent { reduce { state.opened((event.route as EntryReadRoute).entryId) } }
                true
            }

            event is MviNavEvent.NavigateBackTo && event.popUpTo is VaultRootRoute -> {
                intent { reduce { state.copy(groups = listOf(null)) } }
                true
            }

            event is MviNavEvent.NavigateBackTo && event.popUpTo is GroupRoute -> {
                val groupId = (event.popUpTo as GroupRoute).groupId
                intent { reduce { state.backTo(groupId) } }
                true
            }

            // At the root there is nothing above in the container - the app's, as on a phone
            event == MviNavEvent.NavigateBack && container.stateFlow.value.groups.size > 1 -> {
                intent { reduce { state.up() } }
                true
            }

            else -> false
        }
    }

    /**
     * The navigation of the entry [entryId] on the right: going back closes its tab. Its tab closed already - by its
     * cross, while the entry was being deleted - "back" is still the container's: it must not leave `vault`.
     */
    fun entryRouter(entryId: String) = PaneRouter { event ->
        when (event) {
            MviNavEvent.NavigateBack -> {
                intent { reduce { state.closed(entryId) } }
                true
            }

            else -> false
        }
    }

    override fun onSelectTab(entryId: String) {
        intent { reduce { if (state.tabs.any { it.entryId == entryId }) state.copy(active = entryId) else state } }
    }

    override fun onCloseTab(entryId: String) {
        intent { reduce { state.closed(entryId) } }
    }

    override fun onGroupUp() {
        intent { reduce { state.up() } }
    }

    /** The tab of [entryId] shown - opened, unless it is open already. */
    private fun VaultContainerState.opened(entryId: String): VaultContainerState {
        val open = tabs.any { it.entryId == entryId }
        return copy(tabs = if (open) tabs else tabs + EntryTab(entryId), active = entryId)
    }

    /** The tab of [entryId] gone; shown instead - the next one, else the one before. */
    private fun VaultContainerState.closed(entryId: String): VaultContainerState {
        val index = tabs.indexOfFirst { it.entryId == entryId }
        if (index < 0) return this
        val left = tabs - tabs[index]
        val shown = if (active != entryId) active else left.getOrNull(index)?.entryId ?: left.lastOrNull()?.entryId
        return copy(tabs = left, active = shown)
    }

    private fun VaultContainerState.up(): VaultContainerState =
        if (groups.size > 1) copy(groups = groups.dropLast(1)) else this

    /** Back to [groupId] on the way down; not on it - as it was. */
    private fun VaultContainerState.backTo(groupId: String): VaultContainerState {
        val index = groups.indexOf(groupId)
        return if (index < 0) this else copy(groups = groups.take(index + 1))
    }
}
