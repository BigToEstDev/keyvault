package ru.kino.dev.vault.pres.entryhistory

import androidx.lifecycle.ViewModel
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ru.kino.dev.log.ErrorLog
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.Navigator
import ru.kino.dev.vault.changes.VaultChanges
import ru.kino.dev.vault.entry.EntryFailure
import ru.kino.dev.vault.group.EntryId
import ru.kino.dev.vault.group.GroupFailure
import ru.kino.dev.vault.group.GroupRepository
import ru.kino.dev.vault.history.EntryHistoryEditor
import ru.kino.dev.vault.history.EntryHistoryReader
import ru.kino.dev.vault.pres.common.ChangeGate
import ru.kino.dev.vault.pres.common.groupChoice
import ru.kino.dev.vault.pres.navigation.EntryVersionRoute

/**
 * The history of an entry (pass-docs, Step 51, item 3): "Modified" for every old version, newest first, and
 * "Created" last.
 *
 * Read anew every time the screen is shown, and while it is shown on a change made elsewhere - by the list beside
 * it, in a container ([ChangeGate], pass-docs, Step 100). An entry that is gone - deleted or merged away - or in the
 * recycle bin leaves nothing to show: the screen goes back.
 *
 * A version restored - the screen goes back to the entry, which reads itself anew; one deleted - the history
 * is read again, the indexes moved. A change not written to the file is said here, and the history is read
 * again (pass-docs, Step 51, item 4). The whole history deleted - back to the entry (item 5).
 */
class EntryHistoryProcessor(
    private val dbKey: String,
    private val entryId: String,
    private val reader: EntryHistoryReader,
    private val editor: EntryHistoryEditor,
    private val groups: GroupRepository,
    changes: VaultChanges,
    private val navigator: Navigator,
    private val log: ErrorLog,
) : ViewModel(), OrbitContainerHost<EntryHistoryState, EntryHistoryState, Nothing>, EntryHistoryActions {

    private val gate = ChangeGate(changes, dbKey)

    override val container = orbitContainer<EntryHistoryState, Nothing>(EntryHistoryState()) {
        // While the screen is shown: a change made beside it is read again
        repeatOnSubscription { gate.elsewhere.collect { load() } }
    }

    override fun onLoad() {
        intent { load() }
    }

    override fun onBack() {
        intent { navigator.navigate(MviNavEvent.NavigateBack) }
    }

    override fun onOpenVersion(index: Int) {
        intent {
            if (version(index) != null) {
                navigator.navigate(MviNavEvent.NavigateTo(EntryVersionRoute(dbKey, entryId, index)))
            }
        }
    }

    override fun onRestore(index: Int) {
        intent {
            version(index)?.let { modified ->
                if (state.dialog == null) {
                    reduce { state.copy(dialog = EntryHistoryDialog.Restore(index, modified)) }
                }
            }
        }
    }

    override fun onDeleteVersion(index: Int) {
        intent {
            version(index)?.let { modified ->
                if (state.dialog == null) {
                    reduce { state.copy(dialog = EntryHistoryDialog.Delete(index, modified)) }
                }
            }
        }
    }

    override fun onDeleteHistory() {
        intent {
            val anyVersion = state.history?.versions?.isNotEmpty() == true
            if (state.dialog == null && anyVersion) reduce { state.copy(dialog = EntryHistoryDialog.DeleteAll()) }
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

    private suspend fun Syntax<EntryHistoryState, Nothing>.load() {
        gate.reading()
        try {
            val history = reader.read(dbKey, EntryId(entryId))
            if (groups.tree(dbKey).groupChoice(history.groupId) == null) {
                // Not in the visible tree: in the recycle bin, put there beside this screen
                navigator.navigate(MviNavEvent.NavigateBack)
                return
            }
            reduce { state.copy(loading = false, history = history) }
        } catch (_: EntryFailure.EntryNotFound) {
            navigator.navigate(MviNavEvent.NavigateBack)
        } catch (failure: EntryFailure) {
            loadFailed(failure)
        } catch (failure: GroupFailure) {
            loadFailed(failure)
        }
    }

    private suspend fun Syntax<EntryHistoryState, Nothing>.loadFailed(failure: Exception) {
        log.unexpected(TAG, failure)
        reduce { state.copy(loading = false, message = EntryHistoryMessage.LoadFailed) }
    }

    /** When the version at [index] was saved; null when the history shown has no such version. */
    private fun Syntax<EntryHistoryState, Nothing>.version(index: Int): Long? =
        state.history?.versions?.firstOrNull { it.index == index }?.modified

    private suspend fun Syntax<EntryHistoryState, Nothing>.confirm() {
        val dialog = state.dialog ?: return
        if (dialog.saving) return

        reduce { state.copy(dialog = dialog.underWay()) }
        // Its own change: read again or back here, not by the change as well
        gate.own { write(dialog) }
    }

    private suspend fun Syntax<EntryHistoryState, Nothing>.write(dialog: EntryHistoryDialog) {
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
