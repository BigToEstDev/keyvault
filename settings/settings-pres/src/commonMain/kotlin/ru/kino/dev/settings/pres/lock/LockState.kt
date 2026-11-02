package ru.kino.dev.settings.pres.lock

import ru.kino.dev.settings.lock.AutoLockTimeout

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
) {
    /**
     * Asking for biometrics by themselves has something to ask only with biometrics of the database on: off and
     * disabled otherwise, the choice kept as it is (pass-docs, Step 93).
     */
    val autoBiometricPromptAllowed: Boolean = biometric?.on == true
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
    /** No biometrics on the platform at all - the desktop for now. */
    Unsupported,

    /** No strong biometrics on the device. */
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
