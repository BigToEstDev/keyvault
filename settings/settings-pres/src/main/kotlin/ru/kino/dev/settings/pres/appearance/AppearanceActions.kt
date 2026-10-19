package ru.kino.dev.settings.pres.appearance

import ru.kino.dev.settings.appearance.ThemeMode

sealed interface AppearanceAction {

    /** The screen is shown: what is chosen is read. */
    data object Load : AppearanceAction

    data object Back : AppearanceAction

    /** A mode tapped in "Mode": kept at once. */
    data class SelectTheme(val mode: ThemeMode) : AppearanceAction

    /** The snackbar took the message. */
    data object MessageShown : AppearanceAction
}

/** What the snackbar says. */
enum class AppearanceMessage {
    /** The choice could not be written: it is back to what it was. */
    NotSaved,
}
