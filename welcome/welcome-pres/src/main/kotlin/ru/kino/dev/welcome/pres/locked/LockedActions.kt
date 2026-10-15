package ru.kino.dev.welcome.pres.locked

import ru.kino.dev.welcome.BiometricSession
import ru.kino.dev.welcome.pres.common.BiometricOutcome

sealed interface LockedAction {

    data class PasswordChanged(val password: String) : LockedAction

    data object TogglePasswordVisible : LockedAction

    data object Submit : LockedAction

    data object AttachKeyFile : LockedAction

    data class KeyFilePicked(val uri: String) : LockedAction

    /** "Sign in with biometrics" tapped - the prompt is shown only on this. */
    data object Biometric : LockedAction

    data class BiometricFinished(val outcome: BiometricOutcome) : LockedAction
}

sealed interface LockedSideEffect {

    data object PickKeyFile : LockedSideEffect

    data class ShowBiometricPrompt(val session: BiometricSession) : LockedSideEffect
}
