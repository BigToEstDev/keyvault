package ru.kino.dev.settings.pres.lock

import ru.kino.dev.settings.biometric.BiometricSession

sealed interface LockSideEffect {

    /** The system prompt for [session]: its cipher encrypts the password once the biometrics match. */
    data class ShowBiometricPrompt(val session: BiometricSession) : LockSideEffect
}
