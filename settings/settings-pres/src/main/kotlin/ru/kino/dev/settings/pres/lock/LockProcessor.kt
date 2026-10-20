package ru.kino.dev.settings.pres.lock

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
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.core.CoreException
import ru.kino.dev.log.ErrorLog
import ru.kino.dev.mvi.textIntent
import ru.kino.dev.navigation.Navigator
import ru.kino.dev.settings.biometric.BiometricFailure
import ru.kino.dev.settings.biometric.BiometricRepository
import ru.kino.dev.settings.biometric.BiometricSession
import ru.kino.dev.settings.lock.AutoLockTimeout
import ru.kino.dev.settings.lock.InactivityTimer
import ru.kino.dev.settings.lock.LockSettingsRepository
import ru.kino.dev.settings.pres.common.keepSetting
import ru.kino.dev.settings.signin.SignInSettingsRepository
import ru.kino.dev.ui.biometric.BiometricPromptResult
import ru.kino.dev.vault.credentials.DatabaseCredentials

/**
 * @param timeout the chosen time of the auto-lock; null until read - its choice is disabled
 * @param lockOnLeave whether leaving the app locks at once; null until read - its choice is disabled
 * @param lockButton whether the screens have the lock button; null until read - its choice is disabled
 * @param clearPasswordOnError whether a wrong password is wiped; null until read - its choice is disabled
 * @param autoBiometricPrompt whether biometrics are asked by themselves; null until read - its choice is disabled
 * @param biometric the row of biometrics of this database; null until read - disabled
 * @param biometricPassword the password asked before biometrics are turned on
 * @param forgettingBiometric "Forget biometrics for this database?" is open
 * @param choosingTimeout the wheel of the times is open
 * @param message for the snackbar, until it takes it
 */
data class LockState(
    val timeout: AutoLockTimeout? = null,
    val lockOnLeave: Boolean? = null,
    val lockButton: Boolean? = null,
    val clearPasswordOnError: Boolean? = null,
    val autoBiometricPrompt: Boolean? = null,
    val biometric: BiometricRow? = null,
    val biometricPassword: BiometricPasswordDialog? = null,
    val forgettingBiometric: Boolean = false,
    val choosingTimeout: Boolean = false,
    val message: LockMessage? = null,
)

/**
 * How the database locks (pass-docs, Step 59): the time of the auto-lock, locking on leaving the app, the lock
 * button; and how one gets in - asking for biometrics by
 * themselves, wiping a wrong password; biometrics of [dbKey]. A choice
 * is kept at once - the timer of the app takes it from the repository; a new time counts from then on.
 *
 * Biometrics are turned on with the password typed again and checked by the core, then the prompt: only a
 * password that opens the database is kept. It is held here only until the prompt ends. Off - asks first, then
 * forgets (pass-docs, Step 59, item 7).
 */
@HiltViewModel(assistedFactory = LockProcessor.Factory::class)
class LockProcessor @AssistedInject constructor(
    @Assisted private val dbKey: String,
    private val repository: LockSettingsRepository,
    private val signIn: SignInSettingsRepository,
    private val biometric: BiometricRepository,
    private val credentials: DatabaseCredentials,
    private val timer: InactivityTimer,
    private val navigator: Navigator,
    private val log: ErrorLog,
) : ViewModel(), OrbitContainerHost<LockState, LockState, LockSideEffect> {

    @AssistedFactory
    interface Factory {
        fun create(dbKey: String): LockProcessor
    }

    override val container = orbitContainer<LockState, LockSideEffect>(LockState())

    // The checked password, from the dialog until the prompt ends - not in the state, nothing shows it
    private var checkedPassword: String? = null

    fun action(action: LockAction): Job = when (action) {
        // Typed text: the state is updated before the field draws again, or the cursor jumps
        is LockAction.BiometricPasswordChanged -> textIntent {
            reduce { state.copy(biometricPassword = state.biometricPassword?.copy(password = action.password, wrong = false)) }
        }

        else -> intent { handle(action) }
    }

    private suspend fun Syntax<LockState, LockSideEffect>.handle(action: LockAction) {
        when (action) {
            LockAction.Load -> {
                val timeout = repository.autoLockTimeout.first()
                val lockOnLeave = repository.lockOnLeave.first()
                val lockButton = repository.lockButton.first()
                val clearPasswordOnError = signIn.clearPasswordOnError.first()
                val autoBiometricPrompt = signIn.autoBiometricPrompt.first()
                val row = biometricRow()
                reduce {
                    state.copy(
                        timeout = timeout,
                        lockOnLeave = lockOnLeave,
                        lockButton = lockButton,
                        clearPasswordOnError = clearPasswordOnError,
                        autoBiometricPrompt = autoBiometricPrompt,
                        biometric = row,
                    )
                }
            }

            LockAction.Back -> navigator.navigate(MviNavEvent.NavigateBack)

            LockAction.ChooseTimeout -> if (state.timeout != null) reduce { state.copy(choosingTimeout = true) }

            LockAction.DismissTimeout -> reduce { state.copy(choosingTimeout = false) }

            is LockAction.SelectTimeout -> {
                reduce { state.copy(choosingTimeout = false) }
                keepSetting(
                    chosen = action.timeout,
                    current = state.timeout,
                    kept = repository.autoLockTimeout,
                    show = { copy(timeout = it) },
                    notSaved = { copy(message = LockMessage.NotSaved) },
                ) { repository.save(it) }
            }

            is LockAction.SetLockOnLeave -> keepSetting(
                chosen = action.on,
                current = state.lockOnLeave,
                kept = repository.lockOnLeave,
                show = { copy(lockOnLeave = it) },
                notSaved = { copy(message = LockMessage.NotSaved) },
            ) { repository.saveLockOnLeave(it) }

            is LockAction.SetLockButton -> keepSetting(
                chosen = action.on,
                current = state.lockButton,
                kept = repository.lockButton,
                show = { copy(lockButton = it) },
                notSaved = { copy(message = LockMessage.NotSaved) },
            ) { repository.saveLockButton(it) }

            is LockAction.SetClearPasswordOnError -> keepSetting(
                chosen = action.on,
                current = state.clearPasswordOnError,
                kept = signIn.clearPasswordOnError,
                show = { copy(clearPasswordOnError = it) },
                notSaved = { copy(message = LockMessage.NotSaved) },
            ) { signIn.saveClearPasswordOnError(it) }

            is LockAction.SetAutoBiometricPrompt -> keepSetting(
                chosen = action.on,
                current = state.autoBiometricPrompt,
                kept = signIn.autoBiometricPrompt,
                show = { copy(autoBiometricPrompt = it) },
                notSaved = { copy(message = LockMessage.NotSaved) },
            ) { signIn.saveAutoBiometricPrompt(it) }

            is LockAction.SetBiometric -> setBiometric(action.on)

            LockAction.BiometricPasswordConfirm -> confirmPassword()

            LockAction.BiometricPasswordDismiss -> if (state.biometricPassword?.checking == false) {
                reduce { state.copy(biometricPassword = null) }
            }

            is LockAction.BiometricPromptFinished -> promptFinished(action.session, action.result)

            LockAction.BiometricForgetConfirm -> forget()

            LockAction.BiometricForgetDismiss -> reduce { state.copy(forgettingBiometric = false) }

            LockAction.MessageShown -> reduce { state.copy(message = null) }

            // Typed text has its own intent above
            is LockAction.BiometricPasswordChanged -> Unit
        }
    }

    // A database gone meanwhile reads as one without a password: there is nothing to turn on, and the lock
    // takes the screen away anyway
    private suspend fun biometricRow(): BiometricRow {
        val hasPassword = try {
            credentials.hasPassword(dbKey)
        } catch (failure: CoreException) {
            log.unexpected(TAG, failure)
            false
        }
        return BiometricRow(
            on = biometric.isEnabled(dbKey),
            unavailable = when {
                !biometric.isAvailable() -> BiometricUnavailable.NoHardware
                !hasPassword -> BiometricUnavailable.NoPassword
                else -> null
            },
        )
    }

    private suspend fun Syntax<LockState, LockSideEffect>.setBiometric(on: Boolean) {
        val row = state.biometric ?: return
        if (on == row.on) return
        when {
            on && row.unavailable == null -> reduce { state.copy(biometricPassword = BiometricPasswordDialog()) }
            !on -> reduce { state.copy(forgettingBiometric = true) }
        }
    }

    private suspend fun Syntax<LockState, LockSideEffect>.confirmPassword() {
        val dialog = state.biometricPassword ?: return
        if (dialog.checking) return

        reduce { state.copy(biometricPassword = dialog.copy(checking = true)) }
        val matches = try {
            credentials.verifyPassword(dbKey, dialog.password)
        } catch (failure: CoreException) {
            biometricFailed(failure)
            return
        }
        if (!matches) {
            reduce { state.copy(biometricPassword = dialog.copy(checking = false, wrong = true)) }
            return
        }

        val session = try {
            biometric.startSaving()
        } catch (failure: BiometricFailure) {
            biometricFailed(failure)
            return
        }
        checkedPassword = dialog.password
        reduce { state.copy(biometricPassword = null) }
        // The prompt may stop the activity on some phones: that is not leaving the app (pass-docs, Step 59, item 2)
        timer.leavingForSystemScreen()
        postSideEffect(LockSideEffect.ShowBiometricPrompt(session))
    }

    private suspend fun Syntax<LockState, LockSideEffect>.promptFinished(
        session: BiometricSession,
        result: BiometricPromptResult,
    ) {
        val password = checkedPassword ?: return
        checkedPassword = null
        when (result) {
            BiometricPromptResult.Cancelled -> Unit

            BiometricPromptResult.Failed -> reduce { state.copy(message = LockMessage.BiometricFailed) }

            BiometricPromptResult.Succeeded -> try {
                biometric.save(dbKey, password, session)
                reduce { state.copy(biometric = state.biometric?.copy(on = true)) }
            } catch (failure: BiometricFailure) {
                biometricFailed(failure)
            }
        }
    }

    private suspend fun Syntax<LockState, LockSideEffect>.forget() {
        if (!state.forgettingBiometric) return

        try {
            biometric.forget(dbKey)
            reduce { state.copy(forgettingBiometric = false, biometric = state.biometric?.copy(on = false)) }
        } catch (failure: BiometricFailure) {
            reduce { state.copy(forgettingBiometric = false) }
            biometricFailed(failure)
        }
    }

    private suspend fun Syntax<LockState, LockSideEffect>.biometricFailed(failure: Exception) {
        if (failure is BiometricFailure.Unexpected || failure is CoreException) log.unexpected(TAG, failure)
        reduce { state.copy(biometricPassword = null, message = LockMessage.BiometricFailed) }
    }

    private companion object {
        const val TAG = "LockProcessor"
    }
}
