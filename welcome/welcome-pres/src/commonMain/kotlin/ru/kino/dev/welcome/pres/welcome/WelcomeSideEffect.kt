package ru.kino.dev.welcome.pres.welcome

sealed interface WelcomeSideEffect {

    /** Launch the picker of an existing file. */
    data object PickDatabase : WelcomeSideEffect
}
