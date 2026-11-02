package ru.kino.dev.welcome.pres.locked

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.first
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ru.kino.dev.log.ErrorLog
import ru.kino.dev.mvi.textIntent
import ru.kino.dev.navigation.LockedRoute
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.Navigator
import ru.kino.dev.navigation.VaultRootRoute
import ru.kino.dev.settings.signin.SignInSettingsRepository
import ru.kino.dev.welcome.CurrentDatabaseRepository
import ru.kino.dev.welcome.WelcomeDatabaseRepository
import ru.kino.dev.welcome.WelcomeFailure
import ru.kino.dev.welcome.WelcomeFilesRepository
import ru.kino.dev.welcome.pres.common.BiometricNotice
import ru.kino.dev.welcome.pres.common.BiometricOutcome
import ru.kino.dev.welcome.pres.common.BiometricSignIn
import ru.kino.dev.welcome.pres.common.PasswordEntry
import ru.kino.dev.welcome.pres.common.toProblem

/**
 * The database locked while open - its own screen, not a mode of unlocking (pass-docs, `decisions.md`):
 * the core still holds it, so unlocking checks the credentials against the key it kept and is quick. Under
 * this screen there is nothing: the lock popped `vault` whole, and success starts it again at the root
 * (pass-docs, Step 46).
 *
 * Biometrics give the password on a tap, and it unlocks the usual way - `unlock_kdbx` with the password,
 * so the unlock is bound to the Keystore key rather than to a prompt that said yes (pass-docs, Step 33).
 *
 * There is no changing the database here: it is open and may have unsaved changes, and closing it is for
 * `vault`.
 *
 * The screen is laid out as pass-docs `docs/app/mvi-arc.md` says (Step 94): the actions are [LockedActions], done here as methods.
 */
class LockedProcessor(
    private val dbKey: String,
    private val database: WelcomeDatabaseRepository,
    private val files: WelcomeFilesRepository,
    private val current: CurrentDatabaseRepository,
    private val navigator: Navigator,
    private val log: ErrorLog,
    private val biometric: BiometricSignIn,
    private val signIn: SignInSettingsRepository,
) : ViewModel(), OrbitContainerHost<LockedState, LockedState, LockedSideEffect>, LockedActions {

    override val container = orbitContainer<LockedState, LockedSideEffect>(LockedState()) {
        // The key file it was opened with, from the remembered database
        val remembered = current.current.first()?.takeIf { it.uri == dbKey }
        val biometricOn = remembered != null && biometric.isOn(dbKey)
        reduce {
            LockedState(
                databaseName = remembered?.databaseName ?: remembered?.fileName,
                entry = PasswordEntry(
                    keyFileUri = remembered?.keyFileUri,
                    keyFileName = remembered?.keyFileName,
                    biometricOn = biometricOn,
                ),
                read = true,
            )
        }
        // Once, as the lock is shown - not again after a cancel: the button stays (pass-docs, Step 59)
        if (biometricOn && signIn.autoBiometricPrompt.first()) startBiometric()
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

    // The file is read again by the next try itself
    override fun onRetry() {
        intent { submit() }
    }

    override fun onAttachKeyFile() {
        intent { postSideEffect(LockedSideEffect.PickKeyFile) }
    }

    override fun onKeyFilePicked(uri: String) {
        intent {
            val name = try {
                files.displayName(uri)
            } catch (failure: WelcomeFailure) {
                null
            }
            reduce { state.copy(entry = state.entry.withKeyFile(uri, name)) }
        }
    }

    override fun onBiometric() {
        intent { startBiometric() }
    }

    override fun onBiometricFinished(outcome: BiometricOutcome) {
        intent { biometricFinished(outcome) }
    }

    private suspend fun Syntax<LockedState, LockedSideEffect>.startBiometric() {
        if (state.entry.busy) return

        when (val start = biometric.start(dbKey)) {
            is BiometricSignIn.Start.Prompt -> {
                reduce { state.copy(entry = state.entry.copy(biometricNotice = null)) }
                postSideEffect(LockedSideEffect.ShowBiometricPrompt(start.session))
            }

            is BiometricSignIn.Start.Refused -> reduce {
                state.copy(entry = state.entry.copy(biometricOn = start.stillOn, biometricNotice = start.notice))
            }
        }
    }

    private suspend fun Syntax<LockedState, LockedSideEffect>.biometricFinished(outcome: BiometricOutcome) {
        when (outcome) {
            BiometricOutcome.Cancelled -> Unit

            BiometricOutcome.Failed -> reduce {
                state.copy(entry = state.entry.copy(biometricNotice = BiometricNotice.Failed))
            }

            is BiometricOutcome.Succeeded -> {
                val password = biometric.password(dbKey, outcome.session)
                if (password == null) {
                    reduce { state.copy(entry = state.entry.copy(biometricNotice = BiometricNotice.Failed)) }
                } else {
                    unlock(password, fromBiometrics = true)
                }
            }
        }
    }

    private suspend fun Syntax<LockedState, LockedSideEffect>.submit() {
        if (state.entry.busy) return

        unlock(state.entry.passwordForCall, fromBiometrics = false)
    }

    /** Unlocks with [password]; from biometrics, wrong credentials mean the stored password is stale. */
    private suspend fun Syntax<LockedState, LockedSideEffect>.unlock(password: String?, fromBiometrics: Boolean) {
        val entry = state.entry
        reduce { state.copy(entry = entry.copy(busy = true, problem = null, biometricNotice = null)) }
        try {
            database.unlock(dbKey, password, entry.keyFileUri)
            // The screen stays as it is, waiting, until it is gone; what was typed goes with the processor - the
            // locked screen is popped. `vault` was popped by the lock: it starts again at the root (pass-docs,
            // Step 46; Step 40, fix 20)
            navigator.navigate(
                MviNavEvent.NavigateTo(VaultRootRoute(dbKey), popUpTo = LockedRoute(dbKey), inclusive = true),
            )
        } catch (failure: WelcomeFailure) {
            if (fromBiometrics && failure is WelcomeFailure.InvalidCredentials) {
                biometric.passwordRejected(dbKey)
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

    private companion object {
        const val TAG = "LockedProcessor"
    }
}
