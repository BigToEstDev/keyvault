package ru.kino.dev.welcome.pres.biometricsetup

import ru.kino.dev.settings.biometric.BiometricSession

sealed interface BiometricSetupSideEffect {

    data class ShowPrompt(val session: BiometricSession) : BiometricSetupSideEffect
}
