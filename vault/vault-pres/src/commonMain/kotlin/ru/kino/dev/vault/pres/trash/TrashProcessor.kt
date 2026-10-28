package ru.kino.dev.vault.pres.trash

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ru.kino.dev.log.ErrorLog
import ru.kino.dev.mvi.textIntent
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.Navigator
import ru.kino.dev.vault.group.EntryId
import ru.kino.dev.vault.group.GroupFailure
import ru.kino.dev.vault.group.GroupRepository
import ru.kino.dev.vault.pres.common.SEARCH_DELAY
import ru.kino.dev.vault.pres.navigation.TrashEntryRoute
import ru.kino.dev.vault.trash.TrashEditor
import ru.kino.dev.vault.trash.TrashEntry
import ru.kino.dev.vault.trash.TrashFailure
import ru.kino.dev.vault.trash.TrashRepository

/**
 * @param entries what the bin holds; null until read - a skeleton meanwhile
 * @param query the search field
 * @param searching the typing goes on, or the core looks - a skeleton meanwhile
 * @param found what the search found; null - no search, the whole bin
 * @param picked the cards chosen; empty - no choice going on
 * @param dialog the dialog shown, if any
 * @param message for the snackbar, until it takes it
 */
data class TrashState(
    val entries: List<TrashEntry>? = null,
    val query: String = "",
    val searching: Boolean = false,
    val found: List<TrashEntry>? = null,
    val picked: Set<EntryId> = emptySet(),
    val dialog: TrashDialog? = null,
    val message: TrashMessage? = null,
) {
    /** The cards: the last deleted first; deleted in the same second - by title, case aside. */
    val shown: List<TrashEntry> = (found ?: entries.orEmpty()).sortedWith(NEWEST_FIRST)

    val loading: Boolean get() = entries == null

    val choosing: Boolean get() = picked.isNotEmpty()

    /** Why there is no card; null while there are cards, or while something is read. */
    val empty: TrashEmpty?
        get() = when {
            loading || searching || shown.isNotEmpty() -> null
            found != null -> TrashEmpty.NothingFound
            else -> TrashEmpty.Bin
        }

    private companion object {
        val NEWEST_FIRST = compareByDescending<TrashEntry> { it.deletedAt }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.entry.title }
    }
}

/**
 * The recycle bin of the database [dbKey] (pass-docs, Step 70): its entries flat, from any depth of it - groups are
 * never there; the last deleted first. The search is the one of `vault` - the title and the user name, after
 * [SEARCH_DELAY] - over the bin only; the core does it ([TrashRepository.search]).
 *
 * A tap reads the entry (pass-docs, Step 71); a long press starts a choice, and while choosing a tap picks or lets go. The picked entries are
 * restored - all to one group, the one they came from offered when they share it - or removed for good. The whole bin
 * is emptied from the menu of the top bar. Each is one write of the file; after it the bin is read again.
 */
class TrashProcessor(
    private val dbKey: String,
    private val trash: TrashRepository,
    private val editor: TrashEditor,
    private val groups: GroupRepository,
    private val navigator: Navigator,
    private val log: ErrorLog,
) : ViewModel(), OrbitContainerHost<TrashState, TrashState, Nothing> {

    override val container = orbitContainer<TrashState, Nothing>(TrashState())

    /** The search waiting for the typing to stop, or for its answer; the next letter cancels it. */
    private var searchJob: Job? = null

    fun action(action: TrashAction): Job = when (action) {
        is TrashAction.QueryChanged -> queryChanged(action.query)
        else -> intent { handle(action) }
    }

    private suspend fun Syntax<TrashState, Nothing>.handle(action: TrashAction) {
        when (action) {
            is TrashAction.QueryChanged -> Unit // in action()

            TrashAction.Load -> load()

            TrashAction.Back -> if (state.choosing) {
                reduce { state.copy(picked = emptySet()) }
            } else {
                navigator.navigate(MviNavEvent.NavigateBack)
            }

            is TrashAction.LongPress -> pick(action.id)

            is TrashAction.Tap -> if (state.choosing) {
                pick(action.id)
            } else {
                navigator.navigate(MviNavEvent.NavigateTo(TrashEntryRoute(dbKey, action.id.value)))
            }

            TrashAction.ClearChoice -> reduce { state.copy(picked = emptySet()) }

            TrashAction.PickAll -> if (state.choosing) {
                reduce { state.copy(picked = state.shown.map { it.entry.id }.toSet()) }
            }

            TrashAction.Restore -> if (state.choosing && state.dialog == null) askRestore()

            is TrashAction.GroupChosen -> (state.dialog as? TrashDialog.Restore)?.let { dialog ->
                if (!dialog.saving) reduce { state.copy(dialog = dialog.copy(group = action.id)) }
            }

            TrashAction.Remove -> if (state.choosing && state.dialog == null) {
                reduce { state.copy(dialog = TrashDialog.Remove(state.picked.size)) }
            }

            TrashAction.Empty -> if (!state.choosing && state.dialog == null && state.entries.orEmpty().isNotEmpty()) {
                reduce { state.copy(dialog = TrashDialog.Empty()) }
            }

            TrashAction.DialogConfirm -> confirm()

            TrashAction.DialogDismiss -> if (state.dialog?.saving == false) reduce { state.copy(dialog = null) }

            TrashAction.MessageShown -> reduce { state.copy(message = null) }
        }
    }

    private suspend fun Syntax<TrashState, Nothing>.load() {
        val entries = try {
            trash.entries(dbKey)
        } catch (failure: TrashFailure) {
            log.unexpected(TAG, failure)
            reduce { state.copy(entries = state.entries ?: emptyList(), message = TrashMessage.LoadFailed) }
            return
        }
        // What is gone from the bin cannot stay picked
        val ids = entries.map { it.entry.id }.toSet()
        reduce { state.copy(entries = entries, picked = state.picked.intersect(ids)) }
        // Back from elsewhere with a term in the field: what it finds may have changed too
        if (state.query.isNotBlank()) search(state.query)
    }

    private suspend fun Syntax<TrashState, Nothing>.pick(id: EntryId) {
        reduce { state.copy(picked = if (id in state.picked) state.picked - id else state.picked + id) }
    }

    /** The groups to choose from, read now; the one the picked entries came from offered, when they share it. */
    private suspend fun Syntax<TrashState, Nothing>.askRestore() {
        val tree = try {
            groups.tree(dbKey)
        } catch (failure: GroupFailure) {
            log.unexpected(TAG, failure)
            reduce { state.copy(message = TrashMessage.Unexpected) }
            return
        }
        val previous = state.shown.filter { it.entry.id in state.picked }.map { it.previousGroupId }
        reduce { state.copy(dialog = tree.restoreDialog(previous)) }
    }

    private suspend fun Syntax<TrashState, Nothing>.confirm() {
        val dialog = state.dialog ?: return
        if (dialog.saving) return
        val picked = state.picked.toList()
        val change: suspend () -> Unit = when (dialog) {
            is TrashDialog.Restore -> {
                val group = dialog.group ?: return
                ({ editor.restore(dbKey, picked, group) })
            }
            is TrashDialog.Remove -> ({ editor.remove(dbKey, picked) })
            is TrashDialog.Empty -> ({ editor.empty(dbKey) })
        }

        reduce { state.copy(dialog = dialog.saving()) }
        val message = try {
            change()
            null
        } catch (_: TrashFailure.NotSaved) {
            // The database has it: it is written with the next save
            TrashMessage.NotSaved
        } catch (_: TrashFailure.Stale) {
            // Not a bug: the list was older than the bin
            TrashMessage.Changed
        } catch (failure: TrashFailure) {
            log.unexpected(TAG, failure)
            TrashMessage.Unexpected
        }
        reduce { state.copy(dialog = null, picked = emptySet(), message = message) }
        load()
    }

    private fun queryChanged(query: String): Job {
        // Typed text: the state is updated before the field draws again, or the cursor jumps
        val typed = textIntent {
            reduce {
                if (query.isBlank()) {
                    state.copy(query = query, searching = false, found = null)
                } else {
                    state.copy(query = query, searching = true)
                }
            }
        }
        searchJob?.cancel()
        if (query.isBlank()) return typed

        return intent {
            delay(SEARCH_DELAY)
            search(query)
        }.also { searchJob = it }
    }

    private suspend fun Syntax<TrashState, Nothing>.search(term: String) {
        val result = try {
            trash.search(dbKey, term)
        } catch (failure: TrashFailure) {
            log.unexpected(TAG, failure)
            if (term == state.query) reduce { state.copy(searching = false, message = TrashMessage.SearchFailed) }
            return
        }
        // The calls may finish in any order: only the answer to what is in the field now is shown
        if (result.term != state.query) return
        reduce { state.copy(searching = false, found = result.entries) }
    }

    private companion object {
        const val TAG = "TrashProcessor"
    }
}
