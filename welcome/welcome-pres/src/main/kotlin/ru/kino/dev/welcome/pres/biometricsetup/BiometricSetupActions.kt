package ru.kino.dev.welcome.pres.biometricsetup

import ru.kino.dev.welcome.BiometricSession
import ru.kino.dev.welcome.pres.common.BiometricOutcome

sealed interface BiometricSetupAction {

    /** "Turn on": the prompt, then the password of the new database is encrypted and stored. */
    data object Enable : BiometricSetupAction

    data class PromptFinished(val outcome: BiometricOutcome) : BiometricSetupAction

    /** "Skip" - and "back" too: the database is created and open, there is nowhere to go back to. */
    data object Skip : BiometricSetupAction
}

sealed interface BiometricSetupSideEffect {

    data class ShowPrompt(val session: BiometricSession) : BiometricSetupSideEffect
}
