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
import ru.kino.dev.vault.history.EntryHistoryEditor
import ru.kino.dev.vault.history.EntryHistoryReader

/**
 * @param loading the history is not read yet - the skeleton is shown
 * @param history the name of the entry, its old versions newest first and when it was made; null until read
 * @param dialog asking about a version, if any
 * @param message for the snackbar, until it takes it
 */
data class EntryHistoryState(
    val loading: Boolean = true,
    val history: EntryHistory? = null,
    val dialog: EntryHistoryDialog? = null,
    val message: EntryHistoryMessage? = null,
)

/**
 * The history of an entry (pass-docs, Step 51, item 3): "Modified" for every old version, newest first, and
 * "Created" last.
 *
 * Read anew every time the screen is shown. An entry that is gone - deleted or merged away - leaves nothing
 * to show: the screen goes back.
 *
 * A version restored - the screen goes back to the entry, which reads itself anew; one deleted - the history
 * is read again, the indexes moved. A change not written to the file is said here, and the history is read
 * again (pass-docs, Step 51, item 4). The whole history deleted - back to the entry (item 5).
 */
@HiltViewModel(assistedFactory = EntryHistoryProcessor.Factory::class)
class EntryHistoryProcessor @AssistedInject constructor(
    @Assisted("dbKey") private val dbKey: String,
    @Assisted("entryId") private val entryId: String,
    private val reader: EntryHistoryReader,
    private val editor: EntryHistoryEditor,
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

            is EntryHistoryAction.Restore -> version(action.index)?.let { modified ->
                if (state.dialog == null) {
                    reduce { state.copy(dialog = EntryHistoryDialog.Restore(action.index, modified)) }
                }
            }

            is EntryHistoryAction.DeleteVersion -> version(action.index)?.let { modified ->
                if (state.dialog == null) {
                    reduce { state.copy(dialog = EntryHistoryDialog.Delete(action.index, modified)) }
                }
            }

            EntryHistoryAction.DeleteHistory -> {
                val anyVersion = state.history?.versions?.isNotEmpty() == true
                if (state.dialog == null && anyVersion) reduce { state.copy(dialog = EntryHistoryDialog.DeleteAll()) }
            }

            EntryHistoryAction.DialogConfirm -> confirm()

            EntryHistoryAction.DialogDismiss -> if (state.dialog?.saving == false) reduce { state.copy(dialog = null) }

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

    /** When the version at [index] was saved; null when the history shown has no such version. */
    private fun Syntax<EntryHistoryState, Nothing>.version(index: Int): Long? =
        state.history?.versions?.firstOrNull { it.index == index }?.modified

    private suspend fun Syntax<EntryHistoryState, Nothing>.confirm() {
        val dialog = state.dialog ?: return
        if (dialog.saving) return

        reduce { state.copy(dialog = dialog.underWay()) }
        try {
            when (dialog) {
                is EntryHistoryDialog.Restore -> {
                    editor.restore(dbKey, EntryId(entryId), dialog.index)
                    navigator.navigate(MviNavEvent.NavigateBack)
                }

                is EntryHistoryDialog.Delete -> {
                    editor.delete(dbKey, EntryId(entryId), dialog.index)
                    reduce { state.copy(dialog = null) }
                    load()
                }

                is EntryHistoryDialog.DeleteAll -> {
                    editor.deleteAll(dbKey, EntryId(entryId))
                    navigator.navigate(MviNavEvent.NavigateBack)
                }
            }
        } catch (failure: EntryFailure) {
            failed(failure)
        }
    }

    private suspend fun Syntax<EntryHistoryState, Nothing>.failed(failure: EntryFailure) {
        when (failure) {
            // Made, but not in the file: the error is shown here, with the history as it is now
            is EntryFailure.NotSaved -> {
                reduce { state.copy(dialog = null, message = EntryHistoryMessage.NotSaved) }
                load()
            }

            // The version or the entry is gone - the core names neither: reading again shows which
            is EntryFailure.EntryNotFound -> {
                reduce { state.copy(dialog = null) }
                load()
            }

            // Not what changing a history throws
            is EntryFailure.InvalidFields, is EntryFailure.GroupNotFound, is EntryFailure.Unexpected -> {
                log.unexpected(TAG, failure)
                reduce { state.copy(dialog = null, message = EntryHistoryMessage.Unexpected) }
            }
        }
    }

    private companion object {
        const val TAG = "EntryHistoryProcessor"
    }
}

private fun EntryHistoryDialog.underWay(): EntryHistoryDialog = when (this) {
    is EntryHistoryDialog.Restore -> copy(saving = true)
    is EntryHistoryDialog.Delete -> copy(saving = true)
    is EntryHistoryDialog.DeleteAll -> copy(saving = true)
}
