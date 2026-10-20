package ru.kino.dev.settings.pres.lock

import ru.kino.dev.settings.biometric.BiometricSession
import ru.kino.dev.settings.lock.AutoLockTimeout
import ru.kino.dev.ui.biometric.BiometricPromptResult

sealed interface LockAction {

    /** The screen is shown: what is chosen is read. */
    data object Load : LockAction

    data object Back : LockAction

    /** "Auto-lock" tapped: the wheel of its times. */
    data object ChooseTimeout : LockAction

    /** The wheel closed without a choice. */
    data object DismissTimeout : LockAction

    /** A time chosen on the wheel: kept at once. */
    data class SelectTimeout(val timeout: AutoLockTimeout) : LockAction

    /** "Lock on leaving the app" switched: kept at once. */
    data class SetLockOnLeave(val on: Boolean) : LockAction

    /** "Lock button" switched: kept at once. */
    data class SetLockButton(val on: Boolean) : LockAction

    /** "Clear the password after an error" switched: kept at once. */
    data class SetClearPasswordOnError(val on: Boolean) : LockAction

    /** "Ask for biometrics by itself" switched: kept at once. */
    data class SetAutoBiometricPrompt(val on: Boolean) : LockAction

    /** "Biometrics" switched: on - asks for the password first; off - asks to forget them. */
    data class SetBiometric(val on: Boolean) : LockAction

    /** Typed in the password asked before biometrics are turned on. */
    data class BiometricPasswordChanged(val password: String) : LockAction

    /** "Next" under the password: it is checked, then the prompt. */
    data object BiometricPasswordConfirm : LockAction

    data object BiometricPasswordDismiss : LockAction

    /** The prompt shown for [session] ended. */
    data class BiometricPromptFinished(val session: BiometricSession, val result: BiometricPromptResult) : LockAction

    /** "Forget" in the dialog: the password kept for biometrics is gone. */
    data object BiometricForgetConfirm : LockAction

    data object BiometricForgetDismiss : LockAction

    /** The snackbar took the message. */
    data object MessageShown : LockAction
}

sealed interface LockSideEffect {

    /** The system prompt for [session]: its cipher encrypts the password once the biometrics match. */
    data class ShowBiometricPrompt(val session: BiometricSession) : LockSideEffect
}

/** What the snackbar says. */
enum class LockMessage {
    /** The choice could not be written: it shows what is kept. */
    NotSaved,

    /** Biometrics could not be turned on or off - the Keystore refused, or the database is gone. */
    BiometricFailed,
}

/** Why biometrics cannot be turned on for this database - the line under the switch says it. */
enum class BiometricUnavailable {
    /** No strong biometrics on the phone. */
    NoHardware,

    /** The key of the database is a key file alone: there is no password to keep. */
    NoPassword,
}

/**
 * The row of biometrics.
 *
 * @param on the password of this database is kept for biometrics
 * @param unavailable why they cannot be turned on; null - they can
 */
data class BiometricRow(val on: Boolean, val unavailable: BiometricUnavailable? = null)

/**
 * The password asked before biometrics are turned on.
 *
 * @param checking the core checks it: progress on the button, nothing takes taps
 * @param wrong it is not the password of the database - said in the field, the dialog stays
 */
data class BiometricPasswordDialog(
    val password: String = "",
    val checking: Boolean = false,
    val wrong: Boolean = false,
)
