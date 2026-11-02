package ru.kino.dev.settings.pres.lock

import ru.kino.dev.settings.biometric.BiometricSession
import ru.kino.dev.settings.lock.AutoLockTimeout
import ru.kino.dev.ui.biometric.BiometricPromptResult

/** What the lock settings is asked to do - done by [LockProcessor] (pass-docs, `docs/app/mvi-arc.md`). */
interface LockActions {

    /** The screen is shown: what is chosen is read. */
    fun onLoad()

    fun onBack()

    /** "Auto-lock" tapped: the wheel of its times. */
    fun onChooseTimeout()

    /** The wheel closed without a choice. */
    fun onDismissTimeout()

    /** A time chosen on the wheel: kept at once. */
    fun onSelectTimeout(timeout: AutoLockTimeout)

    /** "Lock on leaving the app" switched: kept at once. */
    fun onSetLockOnLeave(on: Boolean)

    /** "Lock button" switched: kept at once. */
    fun onSetLockButton(on: Boolean)

    /** "Clear the password after an error" switched: kept at once. */
    fun onSetClearPasswordOnError(on: Boolean)

    /** "Ask for biometrics by itself" switched: kept at once. */
    fun onSetAutoBiometricPrompt(on: Boolean)

    /** "Biometrics" switched: on - asks for the password first; off - asks to forget them. */
    fun onSetBiometric(on: Boolean)

    /** Typed in the password asked before biometrics are turned on. */
    fun onBiometricPasswordChange(password: String)

    /** "Next" under the password: it is checked, then the prompt. */
    fun onBiometricPasswordConfirm()

    fun onBiometricPasswordDismiss()

    /** The prompt shown for [session] ended. */
    fun onBiometricPromptFinished(session: BiometricSession, result: BiometricPromptResult)

    /** "Forget" in the dialog: the password kept for biometrics is gone. */
    fun onBiometricForgetConfirm()

    fun onBiometricForgetDismiss()

    /** The snackbar took the message. */
    fun onMessageShown()
}
