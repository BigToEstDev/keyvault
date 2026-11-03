package ru.kino.dev.vault.pres.container

import androidx.lifecycle.ViewModel
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ru.kino.dev.core.preset.StandardPreset
import ru.kino.dev.log.ErrorLog
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.VaultRootRoute
import ru.kino.dev.pane.PaneRouter
import ru.kino.dev.vault.changes.VaultChanges
import ru.kino.dev.vault.entry.EntryFailure
import ru.kino.dev.vault.entry.EntryOpener
import ru.kino.dev.vault.group.EntryId
import ru.kino.dev.vault.pres.common.ChangeGate
import ru.kino.dev.vault.pres.navigation.EntryReadRoute
import ru.kino.dev.vault.pres.navigation.GroupRoute

/**
 * The container of `vault` on a wide window: the list of a group on the left, the entries open on the right as tabs
 * (pass-docs, Step 98, `docs/dev/navigation.md`). The screens in it are those of a phone, each with its own
 * processor; what they navigate to that is the container's - another group, an entry, back - comes here through
 * [listRouter] and [entryRouter], the rest goes on to the navigation of the app.
 *
 * A router answers at once, from the kind of the event; the state changes after, in an intent.
 *
 * The title of a tab is read as the reading of the entry reads it - [EntryOpener], its title field - when the tab
 * opens, and again on a change made elsewhere: an entry renamed on its form.
 */
class VaultContainerProcessor(
    private val dbKey: String,
    private val opener: EntryOpener,
    changes: VaultChanges,
    private val log: ErrorLog,
) : ViewModel(), OrbitContainerHost<VaultContainerState, VaultContainerState, Nothing>, VaultContainerActions {

    private val gate = ChangeGate(changes, dbKey)

    override val container = orbitContainer<VaultContainerState, Nothing>(VaultContainerState()) {
        // While the container is shown: the titles of the tabs follow the entries
        repeatOnSubscription { gate.elsewhere.collect { state.tabs.forEach { readTitle(it.entryId) } } }
    }

    override val listRouter = PaneRouter { event ->
        when {
            event is MviNavEvent.NavigateTo && event.route is GroupRoute -> {
                intent { reduce { state.copy(groups = state.groups + (event.route as GroupRoute).groupId) } }
                true
            }

            event is MviNavEvent.NavigateTo && event.route is EntryReadRoute -> {
                open((event.route as EntryReadRoute).entryId)
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

    /** Its tab closed already - by its cross, while the entry was being deleted - "back" is still the container's. */
    override fun entryRouter(entryId: String) = PaneRouter { event ->
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

    private fun open(entryId: String) {
        intent {
            val open = state.tabs.any { it.entryId == entryId }
            reduce { state.copy(tabs = if (open) state.tabs else state.tabs + EntryTab(entryId), active = entryId) }
            if (!open) readTitle(entryId)
        }
    }

    private suspend fun Syntax<VaultContainerState, Nothing>.readTitle(entryId: String) {
        gate.reading()
        val title = try {
            opener.open(dbKey, EntryId(entryId)).content.value(StandardPreset.TITLE)
        } catch (failure: EntryFailure) {
            // Gone - its pane closes the tab; anything else is not what reading an entry throws
            if (failure !is EntryFailure.EntryNotFound) log.unexpected(TAG, failure)
            return
        }
        reduce { state.copy(tabs = state.tabs.map { if (it.entryId == entryId) it.copy(title = title) else it }) }
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

    private companion object {
        const val TAG = "VaultContainerProcessor"
    }
}
