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
import ru.kino.dev.navigation.VaultRootRoute
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.Navigator
import ru.kino.dev.navigation.WelcomeGraph
import ru.kino.dev.welcome.CurrentDatabaseRepository
import ru.kino.dev.welcome.WelcomeDatabaseRepository
import ru.kino.dev.welcome.WelcomeFailure
import ru.kino.dev.welcome.PickedFile
import ru.kino.dev.welcome.WelcomeFilesRepository
import ru.kino.dev.welcome.pres.common.BiometricNotice
import ru.kino.dev.welcome.pres.common.BiometricOutcome
import ru.kino.dev.welcome.pres.common.BiometricSignIn
import ru.kino.dev.welcome.pres.common.EntryProblem
import ru.kino.dev.welcome.pres.common.PasswordEntry
import ru.kino.dev.welcome.pres.common.toProblem
import ru.kino.dev.welcome.pres.navigation.MasterPasswordRoute
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
 * Biometrics, when on for the remembered database, give the password on a tap - and it opens the usual way,
 * with the key file of the database. A stored password that no longer opens it was changed elsewhere: it
 * is forgotten, and the user signs in with the password.
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
    private val biometric: BiometricSignIn,
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

            UnlockAction.Retry -> retry()

            UnlockAction.CreateDatabase -> navigator.navigate(MviNavEvent.NavigateTo(MasterPasswordRoute))

            UnlockAction.ChangeDatabase -> postSideEffect(UnlockSideEffect.PickDatabase)

            is UnlockAction.DatabasePicked -> showDatabase(action.uri)

            UnlockAction.AttachKeyFile -> postSideEffect(UnlockSideEffect.PickKeyFile)

            is UnlockAction.KeyFilePicked -> {
                val name = nameOf(action.uri)
                reduce { state.copy(entry = state.entry.withKeyFile(action.uri, name)) }
            }

            UnlockAction.RemoveKeyFile -> reduce { state.copy(entry = state.entry.withKeyFile(null, null)) }

            UnlockAction.Biometric -> startBiometric()

            is UnlockAction.BiometricFinished -> biometricFinished(action.outcome)
        }
    }

    private suspend fun Syntax<UnlockState, UnlockSideEffect>.startBiometric() {
        val uri = state.databaseUri ?: return
        if (state.entry.busy) return

        when (val start = biometric.start(uri)) {
            is BiometricSignIn.Start.Prompt -> {
                reduce { state.copy(entry = state.entry.copy(biometricNotice = null)) }
                postSideEffect(UnlockSideEffect.ShowBiometricPrompt(start.session))
            }

            is BiometricSignIn.Start.Refused -> reduce {
                state.copy(entry = state.entry.copy(biometricOn = start.stillOn, biometricNotice = start.notice))
            }
        }
    }

    private suspend fun Syntax<UnlockState, UnlockSideEffect>.biometricFinished(outcome: BiometricOutcome) {
        val uri = state.databaseUri ?: return
        when (outcome) {
            BiometricOutcome.Cancelled -> Unit

            BiometricOutcome.Failed -> reduce {
                state.copy(entry = state.entry.copy(biometricNotice = BiometricNotice.Failed))
            }

            is BiometricOutcome.Succeeded -> {
                val password = biometric.password(uri, outcome.session)
                if (password == null) {
                    reduce { state.copy(entry = state.entry.copy(biometricNotice = BiometricNotice.Failed)) }
                } else {
                    open(uri, password, fromBiometrics = true)
                }
            }
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
                entry = PasswordEntry(
                    keyFileUri = remembered.keyFileUri,
                    keyFileName = remembered.keyFileName,
                    biometricOn = biometric.isOn(uri),
                ),
            )
        } else {
            UnlockState(databaseUri = uri, fileName = nameOf(uri))
        }
        // A file deleted outside the app is said now, not after the password was typed for it
        val gone = if (isReachable(uri)) null else DATABASE_GONE
        reduce { next.copy(entry = next.entry.copy(problem = gone)) }
    }

    private suspend fun Syntax<UnlockState, UnlockSideEffect>.retry() {
        val uri = state.databaseUri ?: return
        if (state.entry.busy) return

        reduce { state.copy(entry = state.entry.copy(busy = true)) }
        when {
            !isReachable(uri) -> reduce {
                state.copy(entry = state.entry.copy(busy = false, problem = DATABASE_GONE))
            }
            // Nothing typed yet - the file was missing when the screen came up: now the password can be typed
            state.entry.password.isEmpty() -> reduce {
                state.copy(entry = state.entry.copy(busy = false, problem = null))
            }
            else -> open(uri, state.entry.passwordForCall, fromBiometrics = false)
        }
    }

    // Only a hint, and a failure of asking is no answer: then opening tells
    private suspend fun isReachable(uri: String): Boolean =
        try {
            files.isReachable(uri)
        } catch (failure: WelcomeFailure) {
            true
        }

    private suspend fun Syntax<UnlockState, UnlockSideEffect>.submit() {
        val uri = state.databaseUri ?: return
        if (state.entry.busy) return

        open(uri, state.entry.passwordForCall, fromBiometrics = false)
    }

    /**
     * Opens with [password] and the attached key file. From biometrics, wrong credentials mean the stored
     * password is stale: it is forgotten, and the screen says the password changed instead of "check the
     * password" under a field the user did not type in.
     */
    private suspend fun Syntax<UnlockState, UnlockSideEffect>.open(
        uri: String,
        password: String?,
        fromBiometrics: Boolean,
    ) {
        val entry = state.entry
        reduce { state.copy(entry = entry.copy(busy = true, problem = null, biometricNotice = null)) }
        try {
            val dbKey = database.open(uri, password, entry.keyFileUri)
            // The screen stays as it is, waiting, until it is gone: wiped, it would show itself unopened for the
            // slide into `vault`. What was typed goes with the processor - the graph is popped whole
            // (pass-docs, Step 40, fix 20)
            navigator.navigate(
                MviNavEvent.NavigateTo(VaultRootRoute(dbKey), popUpTo = WelcomeGraph, inclusive = true),
            )
        } catch (failure: WelcomeFailure) {
            if (fromBiometrics && failure is WelcomeFailure.InvalidCredentials) {
                biometric.passwordRejected(uri)
                reduce {
                    state.copy(
                        entry = state.entry.copy(
                            busy = false,
                            biometricOn = false,
                            biometricNotice = BiometricNotice.PasswordChanged,
                        ),
                    )
                }
                return
            }
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

        /** The database file is not there - deleted outside the app, or out of reach. */
        val DATABASE_GONE = EntryProblem.Unreachable(PickedFile.Database)
    }
}

/** The title of the screen: the name from inside the database, else the file name. */
internal val UnlockState.title: String?
    get() = databaseName ?: fileName
