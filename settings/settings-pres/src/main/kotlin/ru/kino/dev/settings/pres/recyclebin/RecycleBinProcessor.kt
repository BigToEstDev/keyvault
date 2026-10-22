package ru.kino.dev.settings.pres.recyclebin

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
import ru.kino.dev.navigation.TrashRoute
import ru.kino.dev.vault.trash.TrashEditor
import ru.kino.dev.vault.trash.TrashFailure
import ru.kino.dev.vault.trash.TrashRepository

/**
 * @param enabled whether the database uses the bin; null until read - the screen is a skeleton meanwhile
 * @param count the entries in the bin
 * @param confirmingOff "switch the bin off?", while asked - the bin holds something
 * @param saving the switch is written into the file - it waits and shows progress
 * @param message for the snackbar, until it takes it
 */
data class RecycleBinState(
    val enabled: Boolean? = null,
    val count: Int = 0,
    val confirmingOff: Boolean = false,
    val saving: Boolean = false,
    val message: RecycleBinMessage? = null,
)

/**
 * The recycle bin of the database [dbKey] (pass-docs, Step 70): whether it is used, and how many entries it holds.
 * Kept in the file, as `Файл базы`: a change is made in the database and the file written at once, with progress;
 * not written - the change is made all the same and waits for the next save.
 *
 * Off, a delete is for good, and the bin goes at once with all it holds - asked first when it holds something.
 */
@HiltViewModel(assistedFactory = RecycleBinProcessor.Factory::class)
class RecycleBinProcessor @AssistedInject constructor(
    @Assisted private val dbKey: String,
    private val trash: TrashRepository,
    private val editor: TrashEditor,
    private val navigator: Navigator,
    private val log: ErrorLog,
) : ViewModel(), OrbitContainerHost<RecycleBinState, RecycleBinState, Nothing> {

    @AssistedFactory
    interface Factory {
        fun create(dbKey: String): RecycleBinProcessor
    }

    override val container = orbitContainer<RecycleBinState, Nothing>(RecycleBinState())

    fun action(action: RecycleBinAction): Job = intent {
        when (action) {
            RecycleBinAction.Load -> load()

            RecycleBinAction.Back -> navigator.navigate(MviNavEvent.NavigateBack)

            is RecycleBinAction.SetEnabled -> setEnabled(action.enabled)

            RecycleBinAction.OffConfirm -> if (state.confirmingOff) {
                reduce { state.copy(confirmingOff = false) }
                write(false)
            }

            RecycleBinAction.OffDismiss -> reduce { state.copy(confirmingOff = false) }

            RecycleBinAction.OpenTrash -> if (state.count > 0) {
                navigator.navigate(MviNavEvent.NavigateTo(TrashRoute(dbKey)))
            }

            RecycleBinAction.MessageShown -> reduce { state.copy(message = null) }
        }
    }

    private suspend fun Syntax<RecycleBinState, Nothing>.load() {
        try {
            val enabled = trash.enabled(dbKey)
            val count = trash.entries(dbKey).size
            reduce { state.copy(enabled = enabled, count = count) }
        } catch (failure: TrashFailure) {
            failed(failure)
        }
    }

    private suspend fun Syntax<RecycleBinState, Nothing>.setEnabled(enabled: Boolean) {
        val current = state.enabled ?: return
        if (state.saving || enabled == current) return

        if (!enabled && state.count > 0) {
            reduce { state.copy(confirmingOff = true) }
        } else {
            write(enabled)
        }
    }

    /** Shows [enabled] at once, while it is written; refused - the switch as it was. */
    private suspend fun Syntax<RecycleBinState, Nothing>.write(enabled: Boolean) {
        val before = state.enabled ?: return
        if (state.saving) return

        reduce { state.copy(enabled = enabled, saving = true) }
        // Off, the bin is gone - made in the database, written or not
        val count = if (enabled) state.count else 0
        try {
            editor.setEnabled(dbKey, enabled)
            reduce { state.copy(count = count, saving = false) }
        } catch (_: TrashFailure.NotSaved) {
            reduce { state.copy(count = count, saving = false, message = RecycleBinMessage.NotSaved) }
        } catch (failure: TrashFailure) {
            reduce { state.copy(enabled = before, saving = false) }
            failed(failure)
        }
    }

    private suspend fun Syntax<RecycleBinState, Nothing>.failed(failure: TrashFailure) {
        log.unexpected(TAG, failure)
        reduce { state.copy(message = RecycleBinMessage.Failed) }
    }

    private companion object {
        const val TAG = "RecycleBinProcessor"
    }
}
