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
import ru.kino.dev.vault.pres.navigation.EntryCreateRoute
import ru.kino.dev.vault.pres.navigation.EntryEditRoute
import ru.kino.dev.vault.pres.navigation.EntryReadRoute
import ru.kino.dev.vault.pres.navigation.GroupRoute

/**
 * The container of `vault` on a wide window: the list of a group on the left, the entries open on the right as tabs
 * (pass-docs, Step 98, `docs/dev/navigation.md`). The screens in it are those of a phone, each with its own
 * processor; what they navigate to that is the container's - another group, an entry, its form, back - comes here
 * through [listRouter], [readRouter] and [formRouter], the rest goes on to the navigation of the app.
 *
 * A tab is one page at a time (item 6): the entry read, or its form - in its place, not over the window; a new entry is
 * made in a tab of its own. The cross of a tab is its "back": a form with changes asks first, as on a phone.
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
) : ViewModel(),
    OrbitContainerHost<VaultContainerState, VaultContainerState, VaultContainerSideEffect>,
    VaultContainerActions {

    private val gate = ChangeGate(changes, dbKey)

    /** The new entries made in the container so far - the tab of each its own key. */
    private var made = 0

    override val container = orbitContainer<VaultContainerState, VaultContainerSideEffect>(VaultContainerState()) {
        // While the container is shown: the titles of the tabs follow the entries
        repeatOnSubscription { gate.elsewhere.collect { state.entries.forEach { readTitle(it.entryId) } } }
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

            event is MviNavEvent.NavigateTo && event.route is EntryCreateRoute -> {
                val groupId = (event.route as EntryCreateRoute).groupId
                intent {
                    val key = newTab(++made)
                    reduce { state.copy(tabs = state.tabs + EntryTab.New(key, groupId), active = key) }
                }
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
     * Its tab closed already - by its cross, while the entry was being deleted - "back" is still the container's; the
     * tab on the form by now - the reading has nothing to close.
     */
    override fun readRouter(entryId: String) = PaneRouter { event ->
        when {
            event == MviNavEvent.NavigateBack -> {
                intent { reduce { if (state.entry(entryId)?.editing == true) state else state.closed(entryId) } }
                true
            }

            event is MviNavEvent.NavigateTo && event.route is EntryEditRoute -> {
                intent { reduce { state.editing(entryId, editing = true) } }
                true
            }

            else -> false
        }
    }

    /** Saved or left: an entry is read again, a new one - its tab closed (it is in the list now); deleted - closed. */
    override fun formRouter(key: String) = PaneRouter { event ->
        when {
            event == MviNavEvent.NavigateBack -> {
                intent {
                    reduce {
                        when (state.tabs.firstOrNull { it.key == key }) {
                            is EntryTab.Entry -> state.editing(key, editing = false)
                            is EntryTab.New -> state.closed(key)
                            null -> state
                        }
                    }
                }
                true
            }

            event is MviNavEvent.NavigateBackTo && event.popUpTo is EntryReadRoute -> {
                intent { reduce { state.closed(key) } }
                true
            }

            else -> false
        }
    }

    override fun onSelectTab(key: String) {
        intent { reduce { if (state.tabs.any { it.key == key }) state.copy(active = key) else state } }
    }

    override fun onCloseTab(key: String) {
        intent {
            val tab = state.tabs.firstOrNull { it.key == key } ?: return@intent
            if (tab is EntryTab.Entry && !tab.editing) {
                // Reading asks nothing before leaving
                reduce { state.closed(key) }
            } else {
                // Shown, so its form can ask; it goes back through its router
                reduce { state.copy(active = key) }
                postSideEffect(VaultContainerSideEffect.PaneBack(tab.pane))
            }
        }
    }

    override fun onGroupUp() {
        intent { reduce { state.up() } }
    }

    private fun open(entryId: String) {
        intent {
            val open = state.entry(entryId) != null
            reduce { state.copy(tabs = if (open) state.tabs else state.tabs + EntryTab.Entry(entryId), active = entryId) }
            if (!open) readTitle(entryId)
        }
    }

    private suspend fun Syntax<VaultContainerState, VaultContainerSideEffect>.readTitle(entryId: String) {
        gate.reading()
        val title = try {
            opener.open(dbKey, EntryId(entryId)).content.value(StandardPreset.TITLE)
        } catch (failure: EntryFailure) {
            // Gone - its pane closes the tab; anything else is not what reading an entry throws
            if (failure !is EntryFailure.EntryNotFound) log.unexpected(TAG, failure)
            return
        }
        reduce { state.withEntry(entryId) { it.copy(title = title) } }
    }

    private val VaultContainerState.entries: List<EntryTab.Entry> get() = tabs.filterIsInstance<EntryTab.Entry>()

    private fun VaultContainerState.entry(entryId: String): EntryTab.Entry? = entries.firstOrNull { it.entryId == entryId }

    private fun VaultContainerState.withEntry(entryId: String, change: (EntryTab.Entry) -> EntryTab.Entry) =
        copy(tabs = tabs.map { if (it is EntryTab.Entry && it.entryId == entryId) change(it) else it })

    private fun VaultContainerState.editing(entryId: String, editing: Boolean) =
        withEntry(entryId) { it.copy(editing = editing) }

    /** The tab [key] gone; shown instead - the next one, else the one before. */
    private fun VaultContainerState.closed(key: String): VaultContainerState {
        val index = tabs.indexOfFirst { it.key == key }
        if (index < 0) return this
        val left = tabs - tabs[index]
        val shown = if (active != key) active else left.getOrNull(index)?.key ?: left.lastOrNull()?.key
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
