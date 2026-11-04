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
import ru.kino.dev.pane.canOpenTab
import ru.kino.dev.pane.closeTab
import ru.kino.dev.vault.changes.VaultChanges
import ru.kino.dev.vault.entry.EntryFailure
import ru.kino.dev.vault.entry.EntryOpener
import ru.kino.dev.vault.group.EntryId
import ru.kino.dev.vault.pres.common.ChangeGate
import ru.kino.dev.vault.pres.navigation.EntryCreateRoute
import ru.kino.dev.vault.pres.navigation.EntryEditRoute
import ru.kino.dev.vault.pres.navigation.EntryHistoryRoute
import ru.kino.dev.vault.pres.navigation.EntryReadRoute
import ru.kino.dev.vault.pres.navigation.EntryVersionRoute
import ru.kino.dev.vault.pres.navigation.GroupRoute

/**
 * The container of `vault` on a wide window: the list of a group on the left, the entries open on the right as tabs
 * (pass-docs, Step 98, `docs/dev/navigation.md`). The screens in it are those of a phone, each with its own
 * processor; what they navigate to that is the container's - another group, an entry, its form, its pages, back - comes here
 * through [listRouter], [entryRouter] and [newRouter], the rest goes on to the navigation of the app.
 *
 * A tab is one page at a time (items 6, 7): the entry read, or a page over it - its form, history, a version - in its
 * place, not over the window; a new entry is made in a tab of its own. The cross of a tab closes it, from any page
 * (item 8); a form with changes asks first, as on "back".
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

            event is MviNavEvent.NavigateTo && event.route is EntryEditRoute -> {
                edit((event.route as EntryEditRoute).entryId)
                true
            }

            event is MviNavEvent.NavigateTo && event.route is EntryCreateRoute -> {
                val groupId = (event.route as EntryCreateRoute).groupId
                intent {
                    if (!state.roomForTab) {
                        reduce { state.copy(tabLimit = true) }
                        return@intent
                    }
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
     * What a page of an entry goes to, as on a phone a screen does (item 7): its form, history, a version - over it;
     * "back" - the page under it, from the reading - the tab closed; back to the reading - a version restored; past it -
     * the entry deleted on its form, the tab closed.
     *
     * Only from the page shown: one under it by now - its tab closed by its cross, a page over it - nothing changes,
     * and the event is still the container's - it does not leave `vault`.
     */
    override fun entryRouter(entryId: String, page: EntryPage?) = PaneRouter { event ->
        val change: (VaultContainerState.() -> VaultContainerState)? = when {
            event == MviNavEvent.NavigateBack ->
                if (page == null) { { closed(entryId) } } else { { withPages(entryId) { it.dropLast(1) } } }

            event is MviNavEvent.NavigateTo -> when (val route = event.route) {
                is EntryEditRoute -> { { withPages(entryId) { it + EntryPage.Edit } } }
                is EntryHistoryRoute -> { { withPages(entryId) { it + EntryPage.History } } }
                is EntryVersionRoute -> { { withPages(entryId) { it + EntryPage.Version(route.index) } } }
                else -> null
            }

            event is MviNavEvent.NavigateBackTo && event.popUpTo is EntryReadRoute ->
                if (event.inclusive) { { closed(entryId) } } else { { withPages(entryId) { emptyList() } } }

            else -> null
        }
        if (change != null) intent { reduce { if (state.entry(entryId)?.top == page) state.change() else state } }
        change != null
    }

    /** Saved: the tab reads the entry made, in its place (item 9); left - closed. */
    override fun newRouter(key: String) = PaneRouter { event ->
        when {
            event == MviNavEvent.NavigateBack -> {
                intent { reduce { state.closed(key) } }
                true
            }

            event is MviNavEvent.NavigateTo && event.route is EntryReadRoute -> {
                val entryId = (event.route as EntryReadRoute).entryId
                intent {
                    reduce { state.created(key, entryId) }
                    readTitle(entryId)
                }
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
            val form = tab is EntryTab.New || (tab is EntryTab.Entry && tab.top == EntryPage.Edit)
            if (form) {
                // Shown, so it can ask; it goes away itself, through its router
                reduce { state.copy(active = key) }
                postSideEffect(VaultContainerSideEffect.ClosePane(tab.pane))
            } else {
                // The other pages ask nothing: the whole tab closes
                reduce { state.closed(key) }
            }
        }
    }

    override fun onGroupUp() {
        intent { reduce { state.up() } }
    }

    override fun onTabLimitDismiss() {
        intent { reduce { state.copy(tabLimit = false) } }
    }

    private fun open(entryId: String) {
        intent {
            val open = state.entry(entryId) != null
            if (!open && !state.roomForTab) {
                reduce { state.copy(tabLimit = true) }
                return@intent
            }
            reduce { state.copy(tabs = if (open) state.tabs else state.tabs + EntryTab.Entry(entryId), active = entryId) }
            if (!open) readTitle(entryId)
        }
    }

    /**
     * "Edit" in the menu of the list (pass-docs, Step 100, item 1): the form of the entry in its tab, over the page
     * shown - "back" from it reads the entry, as from a form opened in it. Not open - its tab opens on the form; on the
     * form already - only shown, its changes kept.
     */
    private fun edit(entryId: String) {
        intent {
            val tab = state.entry(entryId)
            if (tab == null && !state.roomForTab) {
                reduce { state.copy(tabLimit = true) }
                return@intent
            }
            reduce {
                when {
                    tab == null -> state.copy(tabs = state.tabs + EntryTab.Entry(entryId, pages = listOf(EntryPage.Edit)))
                    EntryPage.Edit in tab.pages -> state
                    else -> state.withPages(entryId) { it + EntryPage.Edit }
                }.copy(active = entryId)
            }
            if (tab == null) readTitle(entryId)
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

    /** One more tab opens - [VAULT_MAX_TABS] not reached; none closes by itself to make room (Step 99, item 10). */
    private val VaultContainerState.roomForTab: Boolean get() = canOpenTab(tabs.size, VAULT_MAX_TABS)

    private val VaultContainerState.entries: List<EntryTab.Entry> get() = tabs.filterIsInstance<EntryTab.Entry>()

    private fun VaultContainerState.entry(entryId: String): EntryTab.Entry? = entries.firstOrNull { it.entryId == entryId }

    private fun VaultContainerState.withEntry(entryId: String, change: (EntryTab.Entry) -> EntryTab.Entry) =
        copy(tabs = tabs.map { if (it is EntryTab.Entry && it.entryId == entryId) change(it) else it })

    private fun VaultContainerState.withPages(entryId: String, change: (List<EntryPage>) -> List<EntryPage>) =
        withEntry(entryId) { it.copy(pages = change(it.pages)) }

    /** The new entry of the tab [key] made - [entryId]: the tab reads it, in the same place; shown, if it was. */
    private fun VaultContainerState.created(key: String, entryId: String): VaultContainerState {
        if (tabs.none { it.key == key }) return this
        return copy(
            tabs = tabs.map { if (it.key == key) EntryTab.Entry(entryId) else it },
            active = if (active == key) entryId else active,
        )
    }

    /** The tab [key] gone; shown instead - the next one, else the one before. */
    private fun VaultContainerState.closed(key: String): VaultContainerState =
        closeTab(tabs, active, key, EntryTab::key)?.let { copy(tabs = it.tabs, active = it.active) } ?: this

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
