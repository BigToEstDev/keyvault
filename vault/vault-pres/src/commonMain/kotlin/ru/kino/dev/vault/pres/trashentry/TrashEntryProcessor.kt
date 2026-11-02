package ru.kino.dev.vault.pres.trashentry

import androidx.lifecycle.ViewModel
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ru.kino.dev.core.phone.PhoneNumbers
import ru.kino.dev.log.ErrorLog
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.Navigator
import ru.kino.dev.vault.entries.EntrySettingsRepository
import ru.kino.dev.vault.entry.EntryFailure
import ru.kino.dev.vault.entry.EntryOpener
import ru.kino.dev.vault.group.EntryId
import ru.kino.dev.vault.group.GroupId
import ru.kino.dev.vault.group.GroupFailure
import ru.kino.dev.vault.group.GroupRepository
import ru.kino.dev.vault.pres.common.prepareFor
import ru.kino.dev.vault.pres.common.protectionOrDefault
import ru.kino.dev.vault.pres.common.toggled
import ru.kino.dev.vault.pres.trash.TrashDialog
import ru.kino.dev.vault.pres.trash.restoreDialog
import ru.kino.dev.vault.pres.trash.saving
import ru.kino.dev.vault.trash.TrashEditor
import ru.kino.dev.vault.trash.TrashFailure
import ru.kino.dev.vault.trash.TrashRepository

/**
 * An entry of the recycle bin, read (pass-docs, Step 71): its fields as reading lays them out, the eye on the hidden
 * ones, copying as on reading; no group, no edit, no history. Under the times of the entry - when it was deleted. The
 * fab restores it to a group - the one it came from offered; the bin in the top bar deletes it for good. Either way
 * the screen goes back to the bin, which reads itself anew.
 *
 * An entry no longer in the bin - restored or removed meanwhile - sends the screen back.
 */
class TrashEntryProcessor(
    private val dbKey: String,
    private val entryId: String,
    private val opener: EntryOpener,
    private val settings: EntrySettingsRepository,
    private val trash: TrashRepository,
    private val editor: TrashEditor,
    private val groups: GroupRepository,
    private val phones: PhoneNumbers,
    private val navigator: Navigator,
    private val log: ErrorLog,
) : ViewModel(), OrbitContainerHost<TrashEntryState, TrashEntryState, Nothing>, TrashEntryActions {

    override val container = orbitContainer<TrashEntryState, Nothing>(TrashEntryState())

    private val id = EntryId(entryId)

    override fun onLoad() {
        intent { load() }
    }

    override fun onBack() {
        intent { navigator.navigate(MviNavEvent.NavigateBack) }
    }

    override fun onTogglePassword() {
        intent { reduce { state.copy(passwordVisible = !state.passwordVisible) } }
    }

    override fun onToggleOwn(index: Int) {
        intent { reduce { state.copy(shownOwn = state.shownOwn.toggled(index)) } }
    }

    override fun onToggleKey(key: String) {
        intent { reduce { state.copy(shownKeys = state.shownKeys.toggled(key)) } }
    }

    override fun onRestore() {
        intent { if (state.trashed != null && state.dialog == null) askRestore() }
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
            if (state.trashed != null && state.dialog == null) {
                reduce { state.copy(dialog = TrashDialog.Remove(count = 1)) }
            }
        }
    }

    override fun onDialogConfirm() {
        intent { confirm() }
    }

    override fun onDialogDismiss() {
        intent { if (state.dialog?.saving == false) reduce { state.copy(dialog = null) } }
    }

    override fun onNotSavedSeen() {
        intent { navigator.navigate(MviNavEvent.NavigateBack) }
    }

    override fun onMessageShown() {
        intent { reduce { state.copy(message = null) } }
    }

    private suspend fun Syntax<TrashEntryState, Nothing>.load() {
        // Read once: shown in clear stays shown when the screen comes back
        if (state.entry != null) return
        try {
            val trashed = trash.entries(dbKey).find { it.entry.id == id }
            if (trashed == null) {
                navigator.navigate(MviNavEvent.NavigateBack)
                return
            }
            val entry = opener.open(dbKey, id)
            val protection = settings.protectionOrDefault(dbKey, log, TAG)
            phones.prepareFor(entry.content)
            reduce { state.copy(entry = entry, trashed = trashed, protection = protection) }
        } catch (_: EntryFailure.EntryNotFound) {
            navigator.navigate(MviNavEvent.NavigateBack)
        } catch (failure: EntryFailure) {
            loadFailed(failure)
        } catch (failure: TrashFailure) {
            loadFailed(failure)
        }
    }

    private suspend fun Syntax<TrashEntryState, Nothing>.loadFailed(failure: Exception) {
        log.unexpected(TAG, failure)
        reduce { state.copy(message = TrashEntryMessage.LoadFailed) }
    }

    /** The groups to choose from, read now; the one the entry came from offered. */
    private suspend fun Syntax<TrashEntryState, Nothing>.askRestore() {
        val tree = try {
            groups.tree(dbKey)
        } catch (failure: GroupFailure) {
            log.unexpected(TAG, failure)
            reduce { state.copy(message = TrashEntryMessage.Unexpected) }
            return
        }
        reduce { state.copy(dialog = tree.restoreDialog(listOf(state.trashed?.previousGroupId))) }
    }

    private suspend fun Syntax<TrashEntryState, Nothing>.confirm() {
        val dialog = state.dialog ?: return
        if (dialog.saving) return
        val change: suspend () -> Unit = when (dialog) {
            is TrashDialog.Restore -> {
                val group = dialog.group ?: return
                ({ editor.restore(dbKey, listOf(id), group) })
            }
            is TrashDialog.Remove -> ({ editor.remove(dbKey, listOf(id)) })
            // Not asked here: the whole bin is emptied from the bin
            is TrashDialog.Empty -> return
        }

        reduce { state.copy(dialog = dialog.saving()) }
        try {
            change()
            navigator.navigate(MviNavEvent.NavigateBack)
        } catch (_: TrashFailure.NotSaved) {
            // Made, but not in the file: said here, then back - the entry is not in the bin any more
            reduce { state.copy(dialog = null, notSaved = true) }
        } catch (_: TrashFailure.Stale) {
            // Restored or removed meanwhile, or the group is gone: the bin reads what is there
            navigator.navigate(MviNavEvent.NavigateBack)
        } catch (failure: TrashFailure) {
            log.unexpected(TAG, failure)
            reduce { state.copy(dialog = null, message = TrashEntryMessage.Unexpected) }
        }
    }

    private companion object {
        const val TAG = "TrashEntryProcessor"
    }
}
