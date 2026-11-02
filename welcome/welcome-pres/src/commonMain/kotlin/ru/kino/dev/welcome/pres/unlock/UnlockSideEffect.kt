package ru.kino.dev.welcome.pres.unlock

import ru.kino.dev.settings.biometric.BiometricSession

sealed interface UnlockSideEffect {

    data object PickDatabase : UnlockSideEffect

    data object PickKeyFile : UnlockSideEffect

    data class ShowBiometricPrompt(val session: BiometricSession) : UnlockSideEffect
}
