package ru.kino.dev.settings.pres.appearance

import ru.kino.dev.settings.appearance.AppLanguage
import ru.kino.dev.settings.appearance.ThemeMode

sealed interface AppearanceAction {

    /** The screen is shown: what is chosen is read. */
    data object Load : AppearanceAction

    data object Back : AppearanceAction

    /** A mode tapped in "Mode": kept at once. */
    data class SelectTheme(val mode: ThemeMode) : AppearanceAction

    /** A language picked in "Language": kept at once. */
    data class SelectLanguage(val language: AppLanguage) : AppearanceAction

    /** "Password coloring" switched: kept at once. */
    data class SetPasswordColoring(val on: Boolean) : AppearanceAction

    /** "User name in the list" switched: kept at once. */
    data class SetUserNameInList(val on: Boolean) : AppearanceAction

    /** "Number of entries in a group" switched: kept at once. */
    data class SetEntryCount(val on: Boolean) : AppearanceAction

    /** "Count nested groups" switched: kept at once. */
    data class SetEntryCountNested(val on: Boolean) : AppearanceAction

    /** The snackbar took the message. */
    data object MessageShown : AppearanceAction
}

/** What the snackbar says. */
enum class AppearanceMessage {
    /** The choice could not be written: it is back to what it was. */
    NotSaved,
}
