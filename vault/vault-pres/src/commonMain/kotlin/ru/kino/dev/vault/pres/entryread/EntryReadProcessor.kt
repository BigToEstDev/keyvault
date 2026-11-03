package ru.kino.dev.vault.pres.entryread

import androidx.lifecycle.ViewModel
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ru.kino.dev.core.phone.PhoneNumbers
import ru.kino.dev.core.preset.StandardPreset
import ru.kino.dev.log.ErrorLog
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.Navigator
import ru.kino.dev.vault.changes.VaultChanges
import ru.kino.dev.vault.entries.EntrySettingsRepository
import ru.kino.dev.vault.entry.EntryEditor
import ru.kino.dev.vault.entry.EntryFailure
import ru.kino.dev.vault.entry.EntryOpener
import ru.kino.dev.vault.group.EntryId
import ru.kino.dev.vault.group.GroupFailure
import ru.kino.dev.vault.group.GroupRepository
import ru.kino.dev.vault.lock.AutoLock
import ru.kino.dev.vault.pres.common.ChangeGate
import ru.kino.dev.vault.pres.common.groupChoice
import ru.kino.dev.vault.pres.common.prepareFor
import ru.kino.dev.vault.pres.common.protectionOrDefault
import ru.kino.dev.vault.pres.common.toggled
import ru.kino.dev.vault.pres.navigation.EntryEditRoute
import ru.kino.dev.vault.pres.navigation.EntryHistoryRoute
import ru.kino.dev.vault.trash.TrashRepository
import ru.kino.dev.vault.trash.deletesToBin

/**
 * Reading an entry: its fields as the form lays them out, none of them taking input (pass-docs,
 * vault-our.md "Экран чтения записи", decisions.md "Экраны записи").
 *
 * The entry is read anew every time the screen is shown - it may come back from its form changed. What is
 * shown in clear stays shown while the entry is the same, and hides once it changed: the fields may have
 * moved. A change made elsewhere while the screen is shown - by the list beside it, in a container - is read again
 * too ([ChangeGate], pass-docs, Step 98).
 *
 * An entry in the recycle bin is not shown: the screen goes back. The form deleting the entry goes past this
 * screen itself; this is the guard for an entry put there otherwise (pass-docs, Step 50, item 2). Deleted
 * here, the screen goes back; deleted but not written, it stays and says so.
 */
class EntryReadProcessor(
    private val dbKey: String,
    private val entryId: String,
    private val opener: EntryOpener,
    private val settings: EntrySettingsRepository,
    private val groupRepository: GroupRepository,
    private val editor: EntryEditor,
    private val trash: TrashRepository,
    private val autoLock: AutoLock,
    private val phones: PhoneNumbers,
    private val changes: VaultChanges,
    private val navigator: Navigator,
    private val log: ErrorLog,
) : ViewModel(), OrbitContainerHost<EntryReadState, EntryReadState, Nothing>, EntryReadActions {

    private val gate = ChangeGate(changes, dbKey)

    override val container = orbitContainer<EntryReadState, Nothing>(EntryReadState()) {
        // While the screen is shown: a change made beside it is read again
        repeatOnSubscription { gate.elsewhere.collect { load() } }
    }

    override fun onLoad() {
        intent { load() }
    }

    override fun onBack() {
        intent { navigator.navigate(MviNavEvent.NavigateBack) }
    }

    override fun onEdit() {
        intent {
            if (state.content != null) {
                navigator.navigate(MviNavEvent.NavigateTo(EntryEditRoute(dbKey, entryId)))
            }
        }
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

    override fun onHistory() {
        intent {
            if (state.content != null) {
                navigator.navigate(MviNavEvent.NavigateTo(EntryHistoryRoute(dbKey, entryId)))
            }
        }
    }

    override fun onDelete() {
        intent {
            state.content?.let { content ->
                if (state.delete == null) {
                    val toBin = trash.deletesToBin(dbKey)
                    reduce { state.copy(delete = DeleteDialog(content.value(StandardPreset.TITLE), toBin)) }
                }
            }
        }
    }

    override fun onDeleteConfirm() {
        intent { delete() }
    }

    override fun onDeleteDismiss() {
        intent { if (state.delete?.deleting == false) reduce { state.copy(delete = null) } }
    }

    override fun onLock() {
        intent { lock() }
    }

    override fun onMessageShown() {
        intent { reduce { state.copy(message = null) } }
    }

    private suspend fun Syntax<EntryReadState, Nothing>.lock() {
        if (state.locking) return

        reduce { state.copy(locking = true) }
        // The locked screen is put up by the app, with `vault` gone under it; the refusal is logged by the lock
        if (!autoLock.lockNow()) {
            reduce { state.copy(locking = false, message = EntryReadMessage.Unexpected) }
        }
    }

    private suspend fun Syntax<EntryReadState, Nothing>.delete() {
        val dialog = state.delete ?: return
        if (dialog.deleting) return

        reduce { state.copy(delete = dialog.copy(deleting = true)) }
        // Its own change: the screen goes back, or says it is not written, here - not by the change as well
        gate.own {
            try {
                editor.delete(dbKey, EntryId(entryId))
                navigator.navigate(MviNavEvent.NavigateBack)
            } catch (failure: EntryFailure) {
                deleteFailed(failure)
            }
        }
    }

    private suspend fun Syntax<EntryReadState, Nothing>.deleteFailed(failure: EntryFailure) {
        when (failure) {
            // In the recycle bin, but not in the file: the error is shown here (pass-docs, Step 50, item 2)
            is EntryFailure.NotSaved -> reduce { state.copy(delete = null, message = EntryReadMessage.NotSaved) }

            // Gone already - deleted or merged away
            is EntryFailure.EntryNotFound -> navigator.navigate(MviNavEvent.NavigateBack)

            // Not what deleting an entry throws
            is EntryFailure.InvalidFields, is EntryFailure.GroupNotFound, is EntryFailure.Unexpected -> {
                log.unexpected(TAG, failure)
                reduce { state.copy(delete = null, message = EntryReadMessage.Unexpected) }
            }
        }
    }

    private suspend fun Syntax<EntryReadState, Nothing>.load() {
        gate.reading()
        val opened = try {
            opener.open(dbKey, EntryId(entryId))
        } catch (_: EntryFailure.EntryNotFound) {
            // Deleted or merged away while this screen was under another one
            navigator.navigate(MviNavEvent.NavigateBack)
            return
        } catch (failure: EntryFailure) {
            loadFailed(failure)
            return
        }
        // The group may be another one now - the entry comes back from its form moved
        val group = try {
            groupRepository.tree(dbKey).groupChoice(opened.groupId)
        } catch (failure: GroupFailure) {
            loadFailed(failure)
            return
        }
        if (group == null) {
            // Not in the visible tree: in the recycle bin, put there not by this screen nor by its form
            navigator.navigate(MviNavEvent.NavigateBack)
            return
        }
        val content = opened.content
        // Read anew too: it may come back from the settings changed
        val protection = settings.protectionOrDefault(dbKey, log, TAG)
        phones.prepareFor(content)

        reduce {
            if (content == state.content && protection == state.protection) {
                state.copy(loading = false, group = group, times = opened.times)
            } else {
                state.copy(
                    loading = false,
                    content = content,
                    group = group,
                    times = opened.times,
                    protection = protection,
                    passwordVisible = false,
                    shownOwn = emptySet(),
                    shownKeys = emptySet(),
                )
            }
        }
    }

    private suspend fun Syntax<EntryReadState, Nothing>.loadFailed(failure: Exception) {
        log.unexpected(TAG, failure)
        reduce { state.copy(loading = false, message = EntryReadMessage.LoadFailed) }
    }

    private companion object {
        const val TAG = "EntryReadProcessor"
    }
}
