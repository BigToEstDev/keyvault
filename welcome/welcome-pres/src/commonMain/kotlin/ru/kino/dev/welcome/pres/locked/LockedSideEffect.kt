package ru.kino.dev.welcome.pres.locked

import ru.kino.dev.settings.biometric.BiometricSession

sealed interface LockedSideEffect {

    data object PickKeyFile : LockedSideEffect

    data class ShowBiometricPrompt(val session: BiometricSession) : LockedSideEffect
}
