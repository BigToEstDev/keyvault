package ru.kino.dev.vault.pres.entryhistory

import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ru.kino.dev.log.ErrorLog
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.Navigator
import ru.kino.dev.vault.entry.EntryFailure
import ru.kino.dev.vault.group.EntryId
import ru.kino.dev.vault.history.EntryHistory
import ru.kino.dev.vault.history.EntryHistoryReader

/**
 * @param loading the history is not read yet - the skeleton is shown
 * @param history the name of the entry, its old versions newest first and when it was made; null until read
 * @param message for the snackbar, until it takes it
 */
data class EntryHistoryState(
    val loading: Boolean = true,
    val history: EntryHistory? = null,
    val message: EntryHistoryMessage? = null,
)

/**
 * The history of an entry (pass-docs, Step 51, item 3): "Modified" for every old version, newest first, and
 * "Created" last.
 *
 * Read anew every time the screen is shown. An entry that is gone - deleted or merged away - leaves nothing
 * to show: the screen goes back.
 */
@HiltViewModel(assistedFactory = EntryHistoryProcessor.Factory::class)
class EntryHistoryProcessor @AssistedInject constructor(
    @Assisted("dbKey") private val dbKey: String,
    @Assisted("entryId") private val entryId: String,
    private val reader: EntryHistoryReader,
    private val navigator: Navigator,
    private val log: ErrorLog,
) : ViewModel(), OrbitContainerHost<EntryHistoryState, EntryHistoryState, Nothing> {

    @AssistedFactory
    interface Factory {
        fun create(@Assisted("dbKey") dbKey: String, @Assisted("entryId") entryId: String): EntryHistoryProcessor
    }

    override val container = orbitContainer<EntryHistoryState, Nothing>(EntryHistoryState())

    fun action(action: EntryHistoryAction): Job = intent {
        when (action) {
            EntryHistoryAction.Load -> load()

            EntryHistoryAction.Back -> navigator.navigate(MviNavEvent.NavigateBack)

            EntryHistoryAction.MessageShown -> reduce { state.copy(message = null) }
        }
    }

    private suspend fun Syntax<EntryHistoryState, Nothing>.load() {
        try {
            val history = reader.read(dbKey, EntryId(entryId))
            reduce { state.copy(loading = false, history = history) }
        } catch (_: EntryFailure.EntryNotFound) {
            navigator.navigate(MviNavEvent.NavigateBack)
        } catch (failure: EntryFailure) {
            log.unexpected(TAG, failure)
            reduce { state.copy(loading = false, message = EntryHistoryMessage.LoadFailed) }
        }
    }

    private companion object {
        const val TAG = "EntryHistoryProcessor"
    }
}
