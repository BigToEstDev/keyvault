package ru.kino.dev.vault.pres.trash

import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ru.kino.dev.log.ErrorLog
import ru.kino.dev.mvi.textIntent
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.Navigator
import ru.kino.dev.vault.pres.common.SEARCH_DELAY
import ru.kino.dev.vault.trash.TrashEntry
import ru.kino.dev.vault.trash.TrashFailure
import ru.kino.dev.vault.trash.TrashRepository

/**
 * @param entries what the bin holds; null until read - a skeleton meanwhile
 * @param query the search field
 * @param searching the typing goes on, or the core looks - a skeleton meanwhile
 * @param found what the search found; null - no search, the whole bin
 * @param message for the snackbar, until it takes it
 */
data class TrashState(
    val entries: List<TrashEntry>? = null,
    val query: String = "",
    val searching: Boolean = false,
    val found: List<TrashEntry>? = null,
    val message: TrashMessage? = null,
) {
    /** The cards: the last deleted first; deleted in the same second - by title, case aside. */
    val shown: List<TrashEntry> = (found ?: entries.orEmpty()).sortedWith(NEWEST_FIRST)

    val loading: Boolean get() = entries == null

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
 * never there; the last deleted first. A tap does nothing. The search is the one of `vault` - the title and the user
 * name, after [SEARCH_DELAY] - over the bin only; the core does it ([TrashRepository.search]).
 */
@HiltViewModel(assistedFactory = TrashProcessor.Factory::class)
class TrashProcessor @AssistedInject constructor(
    @Assisted private val dbKey: String,
    private val trash: TrashRepository,
    private val navigator: Navigator,
    private val log: ErrorLog,
) : ViewModel(), OrbitContainerHost<TrashState, TrashState, Nothing> {

    @AssistedFactory
    interface Factory {
        fun create(dbKey: String): TrashProcessor
    }

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

            TrashAction.Back -> navigator.navigate(MviNavEvent.NavigateBack)

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
        reduce { state.copy(entries = entries) }
        // Back from elsewhere with a term in the field: what it finds may have changed too
        if (state.query.isNotBlank()) search(state.query)
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
