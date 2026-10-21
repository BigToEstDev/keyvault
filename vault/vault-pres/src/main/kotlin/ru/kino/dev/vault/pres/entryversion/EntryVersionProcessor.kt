package ru.kino.dev.vault.pres.entryversion

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
import ru.kino.dev.vault.entry.OpenedEntry
import ru.kino.dev.vault.group.EntryId
import ru.kino.dev.vault.history.EntryHistoryEditor
import ru.kino.dev.vault.history.EntryVersionOpener
import ru.kino.dev.vault.pres.common.protectionOrDefault
import ru.kino.dev.vault.pres.common.toggled
import ru.kino.dev.vault.pres.navigation.EntryReadRoute

/**
 * @param loading the version is not read yet - the skeleton is shown
 * @param version the version laid over its preset, with when it was saved; null until it is read
 * @param shownOwn indexes of protected own fields shown in clear
 * @param protection the fields private by the database (pass-docs, Step 65); only the password until read
 * @param shownKeys keys of the fields of the preset private by the database shown in clear
 * @param restoring null - no dialog; false - "restore the version?" asked; true - the restore is under way
 * @param message for the snackbar, until it takes it
 */
data class EntryVersionState(
    val loading: Boolean = true,
    val version: OpenedEntry? = null,
    val passwordVisible: Boolean = false,
    val shownOwn: Set<Int> = emptySet(),
    val protection: FieldProtection = FieldProtection.DEFAULT,
    val shownKeys: Set<String> = emptySet(),
    val restoring: Boolean? = null,
    val message: EntryVersionMessage? = null,
)

/**
 * One old version of an entry, as the entry was then (pass-docs, Step 51, item 6): its fields as reading lays
 * them out, the eye on the hidden ones, nothing copied - restore it to copy. The fab restores it: done, the
 * screen goes to the reading of the entry, past the history; not written to the file - said here.
 *
 * A version or an entry that is gone sends the screen back to the history, which reads itself anew.
 */
@HiltViewModel(assistedFactory = EntryVersionProcessor.Factory::class)
class EntryVersionProcessor @AssistedInject constructor(
    @Assisted("dbKey") private val dbKey: String,
    @Assisted("entryId") private val entryId: String,
    @Assisted("index") private val index: Int,
    private val opener: EntryVersionOpener,
    private val settings: EntrySettingsRepository,
    private val editor: EntryHistoryEditor,
    private val navigator: Navigator,
    private val log: ErrorLog,
) : ViewModel(), OrbitContainerHost<EntryVersionState, EntryVersionState, Nothing> {

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("dbKey") dbKey: String,
            @Assisted("entryId") entryId: String,
            @Assisted("index") index: Int,
        ): EntryVersionProcessor
    }

    override val container = orbitContainer<EntryVersionState, Nothing>(EntryVersionState())

    fun action(action: EntryVersionAction): Job = intent {
        when (action) {
            EntryVersionAction.Load -> load()

            EntryVersionAction.Back -> navigator.navigate(MviNavEvent.NavigateBack)

            EntryVersionAction.TogglePassword -> reduce { state.copy(passwordVisible = !state.passwordVisible) }

            is EntryVersionAction.ToggleOwn -> reduce { state.copy(shownOwn = state.shownOwn.toggled(action.index)) }

            is EntryVersionAction.ToggleKey -> reduce { state.copy(shownKeys = state.shownKeys.toggled(action.key)) }

            EntryVersionAction.Restore -> if (state.version?.times?.modified != null && state.restoring == null) {
                reduce { state.copy(restoring = false) }
            }

            EntryVersionAction.DialogConfirm -> restore()

            EntryVersionAction.DialogDismiss -> if (state.restoring == false) reduce { state.copy(restoring = null) }

            EntryVersionAction.MessageShown -> reduce { state.copy(message = null) }
        }
    }

    private suspend fun Syntax<EntryVersionState, Nothing>.load() {
        // Read once: shown in clear stays shown when the screen comes back
        if (state.version != null) return
        try {
            val version = opener.open(dbKey, EntryId(entryId), index)
            val protection = settings.protectionOrDefault(dbKey, log, TAG)
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
