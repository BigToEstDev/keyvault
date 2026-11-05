package ru.kino.dev.vault.pres.trashentry

import androidx.lifecycle.ViewModel
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ru.kino.dev.core.phone.PhoneNumbers
import ru.kino.dev.log.ErrorLog
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.Navigator
import ru.kino.dev.vault.attachment.AttachmentFiles
import ru.kino.dev.vault.changes.VaultChanges
import ru.kino.dev.vault.entries.EntrySettingsRepository
import ru.kino.dev.vault.entry.EntryFailure
import ru.kino.dev.vault.entry.EntryOpener
import ru.kino.dev.vault.group.EntryId
import ru.kino.dev.vault.group.GroupId
import ru.kino.dev.vault.group.GroupFailure
import ru.kino.dev.vault.group.GroupRepository
import ru.kino.dev.vault.pres.common.ChangeGate
import ru.kino.dev.vault.pres.common.prepareFor
import ru.kino.dev.vault.pres.common.protectionOrDefault
import ru.kino.dev.vault.pres.common.saveAs
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
 * An entry no longer in the bin - restored or removed meanwhile - sends the screen back: when it is read, and while it
 * is shown, on a change made beside it - in the list of the bin, in a container (pass-docs, Step 100, item 2).
 *
 * Its attachments download as on reading ([TrashEntrySideEffect.SaveAttachment], pass-docs, Step 103).
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
    changes: VaultChanges,
    private val attachmentFiles: AttachmentFiles,
    private val navigator: Navigator,
    private val log: ErrorLog,
) : ViewModel(), OrbitContainerHost<TrashEntryState, TrashEntryState, TrashEntrySideEffect>, TrashEntryActions {

    private val gate = ChangeGate(changes, dbKey)

    override val container = orbitContainer<TrashEntryState, TrashEntrySideEffect>(TrashEntryState()) {
        // While the screen is shown: gone from the bin beside it - back
        repeatOnSubscription { gate.elsewhere.collect { checkStillThere() } }
    }

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

    override fun onDownload(index: Int) {
        intent {
            if (state.downloading == null) {
                state.entry?.attachments?.getOrNull(index)?.let { attachment ->
                    reduce { state.copy(saveAs = attachment) }
                    postSideEffect(TrashEntrySideEffect.SaveAttachment(attachment.name))
                }
            }
        }
    }

    override fun onDownloadPicked(uri: String) {
        intent { download(uri) }
    }

    override fun onNotSavedSeen() {
        intent { navigator.navigate(MviNavEvent.NavigateBack) }
    }

    override fun onMessageShown() {
        intent { reduce { state.copy(message = null) } }
    }

    private suspend fun Syntax<TrashEntryState, TrashEntrySideEffect>.download(uri: String) {
        val attachment = state.saveAs ?: return
        if (state.downloading != null) return

        reduce { state.copy(saveAs = null, downloading = attachment) }
        val saved = attachmentFiles.saveAs(dbKey, attachment, uri, log, TAG)
        val message = if (saved) TrashEntryMessage.AttachmentSaved else TrashEntryMessage.AttachmentNotSaved
        reduce { state.copy(downloading = null, message = message) }
    }

    private suspend fun Syntax<TrashEntryState, TrashEntrySideEffect>.load() {
        // Read once: shown in clear stays shown when the screen comes back
        if (state.entry != null) return
        gate.reading()
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

    /** A change made elsewhere: the entry gone from the bin - the screen goes back; still there - shown as it is. */
    private suspend fun Syntax<TrashEntryState, TrashEntrySideEffect>.checkStillThere() {
        gate.reading()
        val there = try {
            trash.entries(dbKey).any { it.entry.id == id }
        } catch (failure: TrashFailure) {
            // Not known: the screen stays, the next change or action tells
            log.unexpected(TAG, failure)
            return
        }
        if (!there) navigator.navigate(MviNavEvent.NavigateBack)
    }

    private suspend fun Syntax<TrashEntryState, TrashEntrySideEffect>.loadFailed(failure: Exception) {
        log.unexpected(TAG, failure)
        reduce { state.copy(message = TrashEntryMessage.LoadFailed) }
    }

    /** The groups to choose from, read now; the one the entry came from offered. */
    private suspend fun Syntax<TrashEntryState, TrashEntrySideEffect>.askRestore() {
        val tree = try {
            groups.tree(dbKey)
        } catch (failure: GroupFailure) {
            log.unexpected(TAG, failure)
            reduce { state.copy(message = TrashEntryMessage.Unexpected) }
            return
        }
        reduce { state.copy(dialog = tree.restoreDialog(listOf(state.trashed?.previousGroupId))) }
    }

    private suspend fun Syntax<TrashEntryState, TrashEntrySideEffect>.confirm() {
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
        // Its own change: the screen goes back here, not by the change as well
        gate.own { write(change) }
    }

    /** [change] made: back to the bin - or what came of it said. */
    private suspend fun Syntax<TrashEntryState, TrashEntrySideEffect>.write(change: suspend () -> Unit) {
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
