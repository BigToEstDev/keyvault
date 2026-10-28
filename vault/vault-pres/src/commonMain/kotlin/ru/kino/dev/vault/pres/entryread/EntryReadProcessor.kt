package ru.kino.dev.vault.pres.entryread

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Job
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ru.kino.dev.core.FieldProtection
import ru.kino.dev.core.phone.PhoneNumbers
import ru.kino.dev.core.preset.StandardPreset
import ru.kino.dev.log.ErrorLog
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.Navigator
import ru.kino.dev.vault.entries.EntrySettingsRepository
import ru.kino.dev.vault.entry.EntryContent
import ru.kino.dev.vault.entry.EntryEditor
import ru.kino.dev.vault.entry.EntryFailure
import ru.kino.dev.vault.entry.EntryOpener
import ru.kino.dev.vault.entry.EntryTimes
import ru.kino.dev.vault.group.EntryId
import ru.kino.dev.vault.group.GroupFailure
import ru.kino.dev.vault.group.GroupRepository
import ru.kino.dev.vault.lock.AutoLock
import ru.kino.dev.vault.pres.common.GroupChoice
import ru.kino.dev.vault.pres.common.groupChoice
import ru.kino.dev.vault.pres.common.prepareFor
import ru.kino.dev.vault.pres.common.protectionOrDefault
import ru.kino.dev.vault.pres.common.toggled
import ru.kino.dev.vault.pres.navigation.EntryEditRoute
import ru.kino.dev.vault.pres.navigation.EntryHistoryRoute
import ru.kino.dev.vault.trash.TrashRepository
import ru.kino.dev.vault.trash.deletesToBin

/**
 * @param loading the entry is not read yet - the skeleton is shown
 * @param content the entry; null until it is read
 * @param group the group the entry is in, with the path down to it; null until it is read
 * @param times when the entry was made and last changed; empty until it is read
 * @param shownOwn indexes of protected own fields shown in clear
 * @param protection the fields private by the database (pass-docs, Step 65); only the password until read
 * @param shownKeys keys of the fields of the preset private by the database shown in clear
 * @param delete the dialog asking to delete the entry
 * @param locking the lock asked by the lock button is under way
 * @param message for the snackbar, until it takes it
 */
data class EntryReadState(
    val loading: Boolean = true,
    val content: EntryContent? = null,
    val group: GroupChoice? = null,
    val times: EntryTimes = EntryTimes(),
    val passwordVisible: Boolean = false,
    val shownOwn: Set<Int> = emptySet(),
    val protection: FieldProtection = FieldProtection.DEFAULT,
    val shownKeys: Set<String> = emptySet(),
    val delete: DeleteDialog? = null,
    val locking: Boolean = false,
    val message: EntryReadMessage? = null,
)

/**
 * Reading an entry: its fields as the form lays them out, none of them taking input (pass-docs,
 * vault-our.md "Экран чтения записи", decisions.md "Экраны записи").
 *
 * The entry is read anew every time the screen is shown - it may come back from its form changed. What is
 * shown in clear stays shown while the entry is the same, and hides once it changed: the fields may have
 * moved.
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
    private val navigator: Navigator,
    private val log: ErrorLog,
) : ViewModel(), OrbitContainerHost<EntryReadState, EntryReadState, Nothing> {

    override val container = orbitContainer<EntryReadState, Nothing>(EntryReadState())

    fun action(action: EntryReadAction): Job = intent {
        when (action) {
            EntryReadAction.Load -> load()

            EntryReadAction.Back -> navigator.navigate(MviNavEvent.NavigateBack)

            EntryReadAction.Edit -> if (state.content != null) {
                navigator.navigate(MviNavEvent.NavigateTo(EntryEditRoute(dbKey, entryId)))
            }

            EntryReadAction.TogglePassword -> reduce { state.copy(passwordVisible = !state.passwordVisible) }

            is EntryReadAction.ToggleOwn -> reduce { state.copy(shownOwn = state.shownOwn.toggled(action.index)) }

            is EntryReadAction.ToggleKey -> reduce { state.copy(shownKeys = state.shownKeys.toggled(action.key)) }

            EntryReadAction.History -> if (state.content != null) {
                navigator.navigate(MviNavEvent.NavigateTo(EntryHistoryRoute(dbKey, entryId)))
            }

            EntryReadAction.Delete -> state.content?.let { content ->
                if (state.delete == null) {
                    val toBin = trash.deletesToBin(dbKey)
                    reduce { state.copy(delete = DeleteDialog(content.value(StandardPreset.TITLE), toBin)) }
                }
            }

            EntryReadAction.DeleteConfirm -> delete()

            EntryReadAction.DeleteDismiss -> if (state.delete?.deleting == false) reduce { state.copy(delete = null) }

            EntryReadAction.Lock -> lock()

            EntryReadAction.MessageShown -> reduce { state.copy(message = null) }
        }
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
        try {
            editor.delete(dbKey, EntryId(entryId))
            navigator.navigate(MviNavEvent.NavigateBack)
        } catch (failure: EntryFailure) {
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
    }

    private suspend fun Syntax<EntryReadState, Nothing>.load() {
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
