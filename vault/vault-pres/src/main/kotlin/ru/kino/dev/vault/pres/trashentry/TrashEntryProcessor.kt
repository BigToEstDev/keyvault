package ru.kino.dev.vault.pres.trashentry

import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ru.kino.dev.core.FieldProtection
import ru.kino.dev.log.ErrorLog
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.Navigator
import ru.kino.dev.vault.entries.EntrySettingsRepository
import ru.kino.dev.vault.entry.EntryFailure
import ru.kino.dev.vault.entry.EntryOpener
import ru.kino.dev.vault.entry.OpenedEntry
import ru.kino.dev.vault.group.EntryId
import ru.kino.dev.vault.group.GroupFailure
import ru.kino.dev.vault.group.GroupRepository
import ru.kino.dev.vault.pres.common.protectionOrDefault
import ru.kino.dev.vault.pres.common.toggled
import ru.kino.dev.vault.pres.trash.TrashDialog
import ru.kino.dev.vault.pres.trash.restoreDialog
import ru.kino.dev.vault.pres.trash.saving
import ru.kino.dev.vault.trash.TrashEditor
import ru.kino.dev.vault.trash.TrashEntry
import ru.kino.dev.vault.trash.TrashFailure
import ru.kino.dev.vault.trash.TrashRepository

/**
 * @param entry the entry laid over its preset, with when it was made and changed; null until read - a skeleton
 * @param trashed when it went to the bin and where from; null until read
 * @param shownOwn indexes of protected own fields shown in clear
 * @param protection the fields private by the database (pass-docs, Step 65); only the password until read
 * @param shownKeys keys of the fields of the preset private by the database shown in clear
 * @param dialog "restore" or "delete for good?", if asked
 * @param notSaved the change is made, but the file was not written - said in a dialog, then back to the bin
 * @param message for the snackbar, until it takes it
 */
data class TrashEntryState(
    val entry: OpenedEntry? = null,
    val trashed: TrashEntry? = null,
    val passwordVisible: Boolean = false,
    val shownOwn: Set<Int> = emptySet(),
    val protection: FieldProtection = FieldProtection.DEFAULT,
    val shownKeys: Set<String> = emptySet(),
    val dialog: TrashDialog? = null,
    val notSaved: Boolean = false,
    val message: TrashEntryMessage? = null,
) {
    val loading: Boolean get() = entry == null
}

/**
 * An entry of the recycle bin, read (pass-docs, Step 71): its fields as reading lays them out, the eye on the hidden
 * ones, copying as on reading; no group, no edit, no history. Under the times of the entry - when it was deleted. The
 * fab restores it to a group - the one it came from offered; the bin in the top bar deletes it for good. Either way
 * the screen goes back to the bin, which reads itself anew.
 *
 * An entry no longer in the bin - restored or removed meanwhile - sends the screen back.
 */
@HiltViewModel(assistedFactory = TrashEntryProcessor.Factory::class)
class TrashEntryProcessor @AssistedInject constructor(
    @Assisted("dbKey") private val dbKey: String,
    @Assisted("entryId") private val entryId: String,
    private val opener: EntryOpener,
    private val settings: EntrySettingsRepository,
    private val trash: TrashRepository,
    private val editor: TrashEditor,
    private val groups: GroupRepository,
    private val navigator: Navigator,
    private val log: ErrorLog,
) : ViewModel(), OrbitContainerHost<TrashEntryState, TrashEntryState, Nothing> {

    @AssistedFactory
    interface Factory {
        fun create(@Assisted("dbKey") dbKey: String, @Assisted("entryId") entryId: String): TrashEntryProcessor
    }

    override val container = orbitContainer<TrashEntryState, Nothing>(TrashEntryState())

    private val id = EntryId(entryId)

    fun action(action: TrashEntryAction): Job = intent {
        when (action) {
            TrashEntryAction.Load -> load()

            TrashEntryAction.Back -> navigator.navigate(MviNavEvent.NavigateBack)

            TrashEntryAction.TogglePassword -> reduce { state.copy(passwordVisible = !state.passwordVisible) }

            is TrashEntryAction.ToggleOwn -> reduce { state.copy(shownOwn = state.shownOwn.toggled(action.index)) }

            is TrashEntryAction.ToggleKey -> reduce { state.copy(shownKeys = state.shownKeys.toggled(action.key)) }

            TrashEntryAction.Restore -> if (state.trashed != null && state.dialog == null) askRestore()

            is TrashEntryAction.GroupChosen -> (state.dialog as? TrashDialog.Restore)?.let { dialog ->
                if (!dialog.saving) reduce { state.copy(dialog = dialog.copy(group = action.id)) }
            }

            TrashEntryAction.Remove -> if (state.trashed != null && state.dialog == null) {
                reduce { state.copy(dialog = TrashDialog.Remove(count = 1)) }
            }

            TrashEntryAction.DialogConfirm -> confirm()

            TrashEntryAction.DialogDismiss -> if (state.dialog?.saving == false) reduce { state.copy(dialog = null) }

            TrashEntryAction.NotSavedSeen -> navigator.navigate(MviNavEvent.NavigateBack)

            TrashEntryAction.MessageShown -> reduce { state.copy(message = null) }
        }
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
