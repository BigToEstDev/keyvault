package ru.kino.dev.welcome.pres.biometricsetup

sealed interface BiometricSetupAction {

    /** "Skip" - and "back" too: the database is created and open, there is nowhere to go back to. */
    data object Skip : BiometricSetupAction
}

/** No side effects yet: enabling biometrics comes with Step 33. */
sealed interface BiometricSetupSideEffect
