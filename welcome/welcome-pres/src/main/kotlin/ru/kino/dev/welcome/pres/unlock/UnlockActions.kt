package ru.kino.dev.welcome.pres.unlock

import ru.kino.dev.welcome.BiometricSession
import ru.kino.dev.welcome.pres.common.BiometricOutcome

sealed interface UnlockAction {

    data class PasswordChanged(val password: String) : UnlockAction

    data object TogglePasswordVisible : UnlockAction

    /** Open the database - also "retry" after a failure that may pass. */
    data object Submit : UnlockAction

    /** The name of the database tapped, or "pick another file" after a failure. */
    data object ChangeDatabase : UnlockAction

    data class DatabasePicked(val uri: String) : UnlockAction

    data object AttachKeyFile : UnlockAction

    data class KeyFilePicked(val uri: String) : UnlockAction

    data object RemoveKeyFile : UnlockAction

    /** "Sign in with biometrics" tapped - the prompt is shown only on this. */
    data object Biometric : UnlockAction

    data class BiometricFinished(val outcome: BiometricOutcome) : UnlockAction
}

sealed interface UnlockSideEffect {

    data object PickDatabase : UnlockSideEffect

    data object PickKeyFile : UnlockSideEffect

    data class ShowBiometricPrompt(val session: BiometricSession) : UnlockSideEffect
}
