package ru.kino.dev.vault.pres.entryversion

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
import ru.kino.dev.vault.group.EntryId
import ru.kino.dev.vault.history.EntryHistoryEditor
import ru.kino.dev.vault.history.EntryVersionOpener
import ru.kino.dev.vault.pres.common.prepareFor
import ru.kino.dev.vault.pres.common.protectionOrDefault
import ru.kino.dev.vault.pres.common.toggled
import ru.kino.dev.vault.pres.navigation.EntryReadRoute

/**
 * One old version of an entry, as the entry was then (pass-docs, Step 51, item 6): its fields as reading lays
 * them out, the eye on the hidden ones, nothing copied - restore it to copy. The fab restores it: done, the
 * screen goes to the reading of the entry, past the history; not written to the file - said here.
 *
 * A version or an entry that is gone sends the screen back to the history, which reads itself anew.
 */
class EntryVersionProcessor(
    private val dbKey: String,
    private val entryId: String,
    private val index: Int,
    private val opener: EntryVersionOpener,
    private val settings: EntrySettingsRepository,
    private val editor: EntryHistoryEditor,
    private val phones: PhoneNumbers,
    private val navigator: Navigator,
    private val log: ErrorLog,
) : ViewModel(), OrbitContainerHost<EntryVersionState, EntryVersionState, Nothing>, EntryVersionActions {

    override val container = orbitContainer<EntryVersionState, Nothing>(EntryVersionState())

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
        intent {
            if (state.version?.times?.modified != null && state.restoring == null) {
                reduce { state.copy(restoring = false) }
            }
        }
    }

    override fun onDialogConfirm() {
        intent { restore() }
    }

    override fun onDialogDismiss() {
        intent { if (state.restoring == false) reduce { state.copy(restoring = null) } }
    }

    override fun onMessageShown() {
        intent { reduce { state.copy(message = null) } }
    }

    private suspend fun Syntax<EntryVersionState, Nothing>.load() {
        // Read once: shown in clear stays shown when the screen comes back
        if (state.version != null) return
        try {
            val version = opener.open(dbKey, EntryId(entryId), index)
            val protection = settings.protectionOrDefault(dbKey, log, TAG)
            phones.prepareFor(version.content)
            reduce { state.copy(loading = false, version = version, protection = protection) }
        } catch (_: EntryFailure.EntryNotFound) {
            navigator.navigate(MviNavEvent.NavigateBack)
        } catch (failure: EntryFailure) {
            log.unexpected(TAG, failure)
            reduce { state.copy(loading = false, message = EntryVersionMessage.LoadFailed) }
        }
    }

    private suspend fun Syntax<EntryVersionState, Nothing>.restore() {
        if (state.restoring != false) return

        reduce { state.copy(restoring = true) }
        try {
            editor.restore(dbKey, EntryId(entryId), index)
            // Past the history: the entry is what was asked for now, and its reading reads itself anew
            navigator.navigate(MviNavEvent.NavigateBackTo(EntryReadRoute(dbKey, entryId), orBack = true))
        } catch (failure: EntryFailure) {
            when (failure) {
                // Made, but not in the file: the error is shown here (pass-docs, Step 51, item 4)
                is EntryFailure.NotSaved -> reduce {
                    state.copy(restoring = null, message = EntryVersionMessage.NotSaved)
                }

                // The version or the entry is gone: the history reads what is there
                is EntryFailure.EntryNotFound -> navigator.navigate(MviNavEvent.NavigateBack)

                // Not what restoring a version throws
                is EntryFailure.InvalidFields, is EntryFailure.GroupNotFound, is EntryFailure.Unexpected -> {
                    log.unexpected(TAG, failure)
                    reduce { state.copy(restoring = null, message = EntryVersionMessage.Unexpected) }
                }
            }
        }
    }

    private companion object {
        const val TAG = "EntryVersionProcessor"
    }
}
