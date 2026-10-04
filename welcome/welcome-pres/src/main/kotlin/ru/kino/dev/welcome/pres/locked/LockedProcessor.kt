package ru.kino.dev.welcome.pres.locked

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
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.Navigator
import ru.kino.dev.welcome.CurrentDatabaseRepository
import ru.kino.dev.welcome.WelcomeDatabaseRepository
import ru.kino.dev.welcome.WelcomeFailure
import ru.kino.dev.welcome.WelcomeFilesRepository
import ru.kino.dev.welcome.pres.common.BiometricNotice
import ru.kino.dev.welcome.pres.common.BiometricOutcome
import ru.kino.dev.welcome.pres.common.BiometricSignIn
import ru.kino.dev.welcome.pres.common.PasswordEntry
import ru.kino.dev.welcome.pres.common.toProblem

/** @param databaseName the name of the database - and nothing else of it is shown while it is locked */
data class LockedState(
    val databaseName: String? = null,
    val entry: PasswordEntry = PasswordEntry(),
)

/**
 * The database locked while open - its own screen, not a mode of unlocking (pass-docs, `decisions.md`):
 * the core still holds it, so unlocking checks the credentials against the key it kept and is quick, and
 * success goes back to where the user was in `vault`, which is still there under this screen.
 *
 * Biometrics give the password on a tap, and it unlocks the usual way - `unlock_kdbx` with the password,
 * so the unlock is bound to the Keystore key rather than to a prompt that said yes (pass-docs, Step 33).
 *
 * There is no changing the database here: it is open and may have unsaved changes, and closing it is for
 * `vault`.
 */
@HiltViewModel(assistedFactory = LockedProcessor.Factory::class)
class LockedProcessor @AssistedInject constructor(
    @Assisted private val dbKey: String,
    private val database: WelcomeDatabaseRepository,
    private val files: WelcomeFilesRepository,
    private val current: CurrentDatabaseRepository,
    private val navigator: Navigator,
    private val log: ErrorLog,
    private val biometric: BiometricSignIn,
) : ViewModel(), OrbitContainerHost<LockedState, LockedState, LockedSideEffect> {

    @AssistedFactory
    interface Factory {
        fun create(dbKey: String): LockedProcessor
    }

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
            )
        }
    }

    fun action(action: LockedAction): Job = when (action) {
        // Typed text: the state is updated before the field draws again, or the cursor jumps
        is LockedAction.PasswordChanged -> textIntent {
            reduce { state.copy(entry = state.entry.typed(action.password)) }
        }

        else -> intent { handle(action) }
    }

    private suspend fun Syntax<LockedState, LockedSideEffect>.handle(action: LockedAction) {
        when (action) {
            is LockedAction.PasswordChanged -> Unit // in action()

            LockedAction.TogglePasswordVisible -> reduce {
                state.copy(entry = state.entry.copy(passwordVisible = !state.entry.passwordVisible))
            }

            LockedAction.Submit -> submit()

            LockedAction.AttachKeyFile -> postSideEffect(LockedSideEffect.PickKeyFile)

            is LockedAction.KeyFilePicked -> {
                val name = try {
                    files.displayName(action.uri)
                } catch (failure: WelcomeFailure) {
                    null
                }
                reduce { state.copy(entry = state.entry.withKeyFile(action.uri, name)) }
            }

            LockedAction.RemoveKeyFile -> reduce { state.copy(entry = state.entry.withKeyFile(null, null)) }

            LockedAction.Biometric -> startBiometric()

            is LockedAction.BiometricFinished -> biometricFinished(action.outcome)
        }
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
            reduce { state.copy(entry = PasswordEntry()) }
            navigator.navigate(MviNavEvent.NavigateBack)
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
            reduce { state.copy(entry = state.entry.copy(busy = false, problem = failure.toProblem())) }
        }
    }

    private companion object {
        const val TAG = "LockedProcessor"
    }
}
