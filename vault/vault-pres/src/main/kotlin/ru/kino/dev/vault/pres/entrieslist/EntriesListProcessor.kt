package ru.kino.dev.vault.pres.entrieslist

import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ru.kino.dev.core.CoreException
import ru.kino.dev.log.ErrorLog
import ru.kino.dev.navigation.LockedRoute
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.Navigator
import ru.kino.dev.vault.VaultDatabaseRepository

/** @param lockFailed locking did not go through - ours, it is in the log */
data class EntriesListState(
    val locking: Boolean = false,
    val lockFailed: Boolean = false,
)

/**
 * The list of entries - a stub for now: the screen of `vault` that is not made yet, over a database that is
 * really open (pass-docs, Step 27).
 *
 * Locking puts the locked screen of `welcome` on top: this screen stays under it, and unlocking comes back
 * here.
 */
@HiltViewModel(assistedFactory = EntriesListProcessor.Factory::class)
class EntriesListProcessor @AssistedInject constructor(
    @Assisted private val dbKey: String,
    private val database: VaultDatabaseRepository,
    private val navigator: Navigator,
    private val log: ErrorLog,
) : ViewModel(), OrbitContainerHost<EntriesListState, EntriesListState, EntriesListSideEffect> {

    @AssistedFactory
    interface Factory {
        fun create(dbKey: String): EntriesListProcessor
    }

    override val container = orbitContainer<EntriesListState, EntriesListSideEffect>(EntriesListState())

    fun action(action: EntriesListAction) = intent {
        when (action) {
            EntriesListAction.Lock -> {
                if (state.locking) return@intent
                reduce { EntriesListState(locking = true) }
                try {
                    database.lock(dbKey)
                    reduce { EntriesListState() }
                    navigator.navigate(MviNavEvent.NavigateTo(LockedRoute(dbKey)))
                } catch (e: CoreException) {
                    // The vault repositories throw what the core throws; no screen answer of its own yet
                    log.unexpected(TAG, e)
                    reduce { EntriesListState(lockFailed = true) }
                }
            }
        }
    }

    private companion object {
        const val TAG = "EntriesListProcessor"
    }
}
