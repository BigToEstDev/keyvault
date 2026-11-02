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
import ru.kino.dev.vault.group.GroupId
import ru.kino.dev.vault.group.GroupRepository
import ru.kino.dev.vault.pres.common.SEARCH_DELAY
import ru.kino.dev.vault.pres.navigation.TrashEntryRoute
import ru.kino.dev.vault.trash.TrashEditor
import ru.kino.dev.vault.trash.TrashFailure
import ru.kino.dev.vault.trash.TrashRepository

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
) : ViewModel(), OrbitContainerHost<TrashState, TrashState, Nothing>, TrashActions {

    override val container = orbitContainer<TrashState, Nothing>(TrashState())

    /** The search waiting for the typing to stop, or for its answer; the next letter cancels it. */
    private var searchJob: Job? = null

    override fun onLoad() {
        intent { load() }
    }

    override fun onBack() {
        intent {
            if (state.choosing) {
                reduce { state.copy(picked = emptySet()) }
            } else {
                navigator.navigate(MviNavEvent.NavigateBack)
            }
        }
    }

    override fun onQueryChange(query: String) {
        // Typed text: the state is updated before the field draws again, or the cursor jumps
        textIntent {
            reduce {
                if (query.isBlank()) {
                    state.copy(query = query, searching = false, found = null)
                } else {
                    state.copy(query = query, searching = true)
                }
            }
        }
        searchJob?.cancel()
        if (query.isBlank()) return

        searchJob = intent {
            delay(SEARCH_DELAY)
            search(query)
        }
    }

    override fun onLongPress(id: EntryId) {
        intent { pick(id) }
    }

    override fun onTap(id: EntryId) {
        intent {
            if (state.choosing) {
                pick(id)
            } else {
                navigator.navigate(MviNavEvent.NavigateTo(TrashEntryRoute(dbKey, id.value)))
            }
        }
    }

    override fun onClearChoice() {
        intent { reduce { state.copy(picked = emptySet()) } }
    }

    override fun onPickAll() {
        intent { if (state.choosing) reduce { state.copy(picked = state.shown.map { it.entry.id }.toSet()) } }
    }

    override fun onRestore() {
        intent { if (state.choosing && state.dialog == null) askRestore() }
    }

    override fun onGroupChosen(id: GroupId) {
        intent {
            (state.dialog as? TrashDialog.Restore)?.let { dialog ->
                if (!dialog.saving) reduce { state.copy(dialog = dialog.copy(group = id)) }
            }
        }
    }

    override fun onRemove() {
        intent {
            if (state.choosing && state.dialog == null) reduce { state.copy(dialog = TrashDialog.Remove(state.picked.size)) }
        }
    }

    override fun onEmpty() {
        intent {
            if (!state.choosing && state.dialog == null && state.entries.orEmpty().isNotEmpty()) {
                reduce { state.copy(dialog = TrashDialog.Empty()) }
            }
        }
    }

    override fun onDialogConfirm() {
        intent { confirm() }
    }

    override fun onDialogDismiss() {
        intent { if (state.dialog?.saving == false) reduce { state.copy(dialog = null) } }
    }

    override fun onMessageShown() {
        intent { reduce { state.copy(message = null) } }
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
