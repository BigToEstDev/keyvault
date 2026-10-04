package ru.kino.dev.welcome.pres.welcome

sealed interface WelcomeAction {

    data object OpenClicked : WelcomeAction

    data class DatabasePicked(val uri: String) : WelcomeAction

    data object CreateClicked : WelcomeAction
}

sealed interface WelcomeSideEffect {

    /** Launch the picker of an existing file. */
    data object PickDatabase : WelcomeSideEffect
}
