package ru.kino.dev.welcome.pres.unlock

import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ru.kino.dev.log.ErrorLog
import ru.kino.dev.mvi.textIntent
import ru.kino.dev.navigation.EntriesListRoute
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.Navigator
import ru.kino.dev.navigation.WelcomeGraph
import ru.kino.dev.welcome.CurrentDatabaseRepository
import ru.kino.dev.welcome.WelcomeDatabaseRepository
import ru.kino.dev.welcome.WelcomeFailure
import ru.kino.dev.welcome.WelcomeFilesRepository
import ru.kino.dev.welcome.pres.common.PasswordEntry
import ru.kino.dev.welcome.pres.common.toProblem
import ru.kino.dev.welcome.pres.navigation.WelcomeRoute

/**
 * @param databaseUri the database to open; null until it is known, and when nothing is remembered
 * @param databaseName the name from inside the database, known once it was opened before
 * @param fileName the name of the file, shown when the database has not been opened yet
 */
data class UnlockState(
    val databaseUri: String? = null,
    val databaseName: String? = null,
    val fileName: String? = null,
    val entry: PasswordEntry = PasswordEntry(),
)

/**
 * Opening a database that is not open - on start, the remembered one; after a file was picked, that one.
 *
 * Opening reads the file and runs the kdf - the 2.5 s wait. Only a database that opened is remembered, so
 * a file picked here by mistake replaces nothing; the repository does the remembering, this screen only
 * goes on to `vault` and wipes the `welcome` graph behind it.
 *
 * @param pickedUri a file just picked, or null for the remembered database
 */
@HiltViewModel(assistedFactory = UnlockProcessor.Factory::class)
class UnlockProcessor @AssistedInject constructor(
    @Assisted private val pickedUri: String?,
    private val database: WelcomeDatabaseRepository,
    private val files: WelcomeFilesRepository,
    private val current: CurrentDatabaseRepository,
    private val navigator: Navigator,
    private val log: ErrorLog,
) : ViewModel(), OrbitContainerHost<UnlockState, UnlockState, UnlockSideEffect> {

    @AssistedFactory
    interface Factory {
        fun create(pickedUri: String?): UnlockProcessor
    }

    override val container = orbitContainer<UnlockState, UnlockSideEffect>(UnlockState()) {
        if (pickedUri != null) {
            showDatabase(pickedUri)
        } else {
            val remembered = current.current.first()
            if (remembered == null) {
                // Nothing to unlock - the remembered database is gone; start over
                navigator.navigate(MviNavEvent.NavigateTo(WelcomeRoute, popUpTo = WelcomeGraph))
            } else {
                showDatabase(remembered.uri)
            }
        }
    }

    fun action(action: UnlockAction): Job = when (action) {
        // Typed text: the state is updated before the field draws again, or the cursor jumps
        is UnlockAction.PasswordChanged -> textIntent {
            reduce { state.copy(entry = state.entry.typed(action.password)) }
        }

        else -> intent { handle(action) }
    }

    private suspend fun Syntax<UnlockState, UnlockSideEffect>.handle(action: UnlockAction) {
        when (action) {
            is UnlockAction.PasswordChanged -> Unit // in action()

            UnlockAction.TogglePasswordVisible -> reduce {
                state.copy(entry = state.entry.copy(passwordVisible = !state.entry.passwordVisible))
            }

            UnlockAction.Submit -> submit()

            UnlockAction.ChangeDatabase -> postSideEffect(UnlockSideEffect.PickDatabase)

            is UnlockAction.DatabasePicked -> showDatabase(action.uri)

            UnlockAction.AttachKeyFile -> postSideEffect(UnlockSideEffect.PickKeyFile)

            is UnlockAction.KeyFilePicked -> {
                val name = nameOf(action.uri)
                reduce { state.copy(entry = state.entry.withKeyFile(action.uri, name)) }
            }

            UnlockAction.RemoveKeyFile -> reduce { state.copy(entry = state.entry.withKeyFile(null, null)) }
        }
    }

    /**
     * Shows [uri] as the database to open. The remembered one comes with its names and its key file; any
     * other one with the name of the file and no key file - and nothing of what was typed for another one.
     */
    private suspend fun Syntax<UnlockState, UnlockSideEffect>.showDatabase(uri: String) {
        val remembered = current.current.first()?.takeIf { it.uri == uri }
        val next = if (remembered != null) {
            UnlockState(
                databaseUri = uri,
                databaseName = remembered.databaseName,
                fileName = remembered.fileName,
                entry = PasswordEntry(keyFileUri = remembered.keyFileUri, keyFileName = remembered.keyFileName),
            )
        } else {
            UnlockState(databaseUri = uri, fileName = nameOf(uri))
        }
        reduce { next }
    }

    private suspend fun Syntax<UnlockState, UnlockSideEffect>.submit() {
        val uri = state.databaseUri ?: return
        if (state.entry.busy) return

        val entry = state.entry
        reduce { state.copy(entry = entry.copy(busy = true, problem = null)) }
        try {
            val dbKey = database.open(uri, entry.passwordForCall, entry.keyFileUri)
            // Nothing typed stays behind, though the screen is about to go
            reduce { state.copy(entry = PasswordEntry()) }
            navigator.navigate(
                MviNavEvent.NavigateTo(EntriesListRoute(dbKey), popUpTo = WelcomeGraph, inclusive = true),
            )
        } catch (failure: WelcomeFailure) {
            if (failure is WelcomeFailure.Unexpected) log.unexpected(TAG, failure)
            reduce { state.copy(entry = state.entry.copy(busy = false, problem = failure.toProblem())) }
        }
    }

    // A name the provider does not tell is shown as nothing rather than as a failure: the file may still open
    private suspend fun nameOf(uri: String): String? =
        try {
            files.displayName(uri)
        } catch (failure: WelcomeFailure) {
            null
        }

    private companion object {
        const val TAG = "UnlockProcessor"
    }
}

/** The title of the screen: the name from inside the database, else the file name. */
internal val UnlockState.title: String?
    get() = databaseName ?: fileName
