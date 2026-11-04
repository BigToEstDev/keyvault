package ru.kino.dev.vault.pres.trashcontainer

import androidx.lifecycle.ViewModel
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ru.kino.dev.core.preset.StandardPreset
import ru.kino.dev.log.ErrorLog
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.pane.PaneRouter
import ru.kino.dev.pane.canOpenTab
import ru.kino.dev.pane.closeTab
import ru.kino.dev.vault.entry.EntryFailure
import ru.kino.dev.vault.entry.EntryOpener
import ru.kino.dev.vault.group.EntryId
import ru.kino.dev.vault.pres.navigation.TrashEntryRoute

/**
 * The container of the recycle bin on a wide window (pass-docs, Step 100, item 2, `docs/dev/navigation.md`): the list
 * of the bin on the left, its entries open on the right as tabs - as the container of `vault`, in the place of the bin
 * on the back stack. The screens in it are those of a phone, each with its own processor; what they navigate to that
 * is the container's comes here through [listRouter] and [entryRouter], the rest goes on to the navigation of the app -
 * "back" of the list too: back to `vault`.
 *
 * An entry of the bin is only read: no pages over it. Restored, removed for good, gone from the bin - its screen goes
 * back, and the tab closes.
 *
 * The title of a tab is read as the reading of the entry reads it - [EntryOpener], its title field - when the tab
 * opens: an entry in the bin is not edited.
 */
class TrashContainerProcessor(
    private val dbKey: String,
    private val opener: EntryOpener,
    private val log: ErrorLog,
) : ViewModel(),
    OrbitContainerHost<TrashContainerState, TrashContainerState, Nothing>,
    TrashContainerActions {

    override val container = orbitContainer<TrashContainerState, Nothing>(TrashContainerState())

    override val listRouter = PaneRouter { event ->
        if (event is MviNavEvent.NavigateTo && event.route is TrashEntryRoute) {
            open((event.route as TrashEntryRoute).entryId)
            true
        } else {
            false
        }
    }

    /** "Back" of an entry closes its tab - closed by its cross already, nothing changes: it does not leave the bin. */
    override fun entryRouter(entryId: String) = PaneRouter { event ->
        if (event == MviNavEvent.NavigateBack) {
            intent { reduce { state.closed(entryId) } }
            true
        } else {
            false
        }
    }

    override fun onSelectTab(entryId: String) {
        intent { reduce { if (state.tabs.any { it.entryId == entryId }) state.copy(active = entryId) else state } }
    }

    override fun onCloseTab(entryId: String) {
        intent { reduce { state.closed(entryId) } }
    }

    override fun onTabLimitDismiss() {
        intent { reduce { state.copy(tabLimit = false) } }
    }

    /** Open already - shown; else a tab of its own, while [TRASH_MAX_TABS] is not reached (Step 99, item 10). */
    private fun open(entryId: String) {
        intent {
            val open = state.tabs.any { it.entryId == entryId }
            if (!open && !canOpenTab(state.tabs.size, TRASH_MAX_TABS)) {
                reduce { state.copy(tabLimit = true) }
                return@intent
            }
            reduce { state.copy(tabs = if (open) state.tabs else state.tabs + TrashTab(entryId), active = entryId) }
            if (!open) readTitle(entryId)
        }
    }

    private suspend fun Syntax<TrashContainerState, Nothing>.readTitle(entryId: String) {
        val title = try {
            opener.open(dbKey, EntryId(entryId)).content.value(StandardPreset.TITLE)
        } catch (failure: EntryFailure) {
            // Gone - its pane closes the tab; anything else is not what reading an entry throws
            if (failure !is EntryFailure.EntryNotFound) log.unexpected(TAG, failure)
            return
        }
        reduce { state.copy(tabs = state.tabs.map { if (it.entryId == entryId) it.copy(title = title) else it }) }
    }

    /** The tab [entryId] gone; shown instead - the next one, else the one before. */
    private fun TrashContainerState.closed(entryId: String): TrashContainerState =
        closeTab(tabs, active, entryId, TrashTab::entryId)?.let { copy(tabs = it.tabs, active = it.active) } ?: this

    private companion object {
        const val TAG = "TrashContainerProcessor"
    }
}
