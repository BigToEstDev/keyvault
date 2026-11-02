package ru.kino.dev.welcome.pres.unlock

import androidx.lifecycle.ViewModel
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
import ru.kino.dev.settings.signin.SignInSettingsRepository
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
 * The screen is laid out as pass-docs `docs/app/mvi-arc.md` says (Step 94): the actions are [UnlockActions], done here as methods.
 *
 * @param pickedUri a file just picked, or null for the remembered database
 */
class UnlockProcessor(
    private val pickedUri: String?,
    private val database: WelcomeDatabaseRepository,
    private val files: WelcomeFilesRepository,
    private val current: CurrentDatabaseRepository,
    private val navigator: Navigator,
    private val log: ErrorLog,
    private val biometric: BiometricSignIn,
    private val signIn: SignInSettingsRepository,
) : ViewModel(), OrbitContainerHost<UnlockState, UnlockState, UnlockSideEffect>, UnlockActions {

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
        // Once, as the screen is shown, for the remembered database there - not for one picked later, not again
        // after a cancel: the button stays (pass-docs, Step 59)
        val entry = state.entry
        if (entry.biometricOn && entry.problem == null && signIn.autoBiometricPrompt.first()) startBiometric()
    }

    // Typed text: the state is updated before the field draws again, or the cursor jumps
    override fun onPasswordChange(password: String) {
        textIntent { reduce { state.copy(entry = state.entry.typed(password)) } }
    }

    override fun onTogglePasswordVisible() {
        intent { reduce { state.copy(entry = state.entry.copy(passwordVisible = !state.entry.passwordVisible)) } }
    }

    override fun onSubmit() {
        intent { submit() }
    }

    /**
     * The file is looked at again first - it may have been found by the provider - and opened only when something
     * was typed for it.
     */
    override fun onRetry() {
        intent { retry() }
    }

    override fun onCreateDatabase() {
        intent { navigator.navigate(MviNavEvent.NavigateTo(MasterPasswordRoute)) }
    }

    override fun onChangeDatabase() {
        intent { postSideEffect(UnlockSideEffect.PickDatabase) }
    }

    override fun onDatabasePicked(uri: String) {
        intent { showDatabase(uri) }
    }

    override fun onAttachKeyFile() {
        intent { postSideEffect(UnlockSideEffect.PickKeyFile) }
    }

    override fun onKeyFilePicked(uri: String) {
        intent {
            val name = nameOf(uri)
            reduce { state.copy(entry = state.entry.withKeyFile(uri, name)) }
        }
    }

    override fun onRemoveKeyFile() {
        intent { reduce { state.copy(entry = state.entry.withKeyFile(null, null)) } }
    }

    override fun onBiometric() {
        intent { startBiometric() }
    }

    override fun onBiometricFinished(outcome: BiometricOutcome) {
        intent { biometricFinished(outcome) }
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
                read = true,
            )
        } else {
            UnlockState(databaseUri = uri, fileName = nameOf(uri), read = true)
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
            // A wrong password - or key file: the core does not tell which - wiped when the settings say so
            val clear = failure is WelcomeFailure.InvalidCredentials && signIn.clearPasswordOnError.first()
            reduce { state.copy(entry = state.entry.failed(failure.toProblem(), clearPassword = clear)) }
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
