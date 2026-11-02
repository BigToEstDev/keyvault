package ru.kino.dev.settings.pres.appearance

import ru.kino.dev.settings.appearance.AppLanguage
import ru.kino.dev.settings.appearance.ThemeMode

/** What the appearance settings is asked to do - done by [AppearanceProcessor] (pass-docs, `docs/app/mvi-arc.md`). */
interface AppearanceActions {

    /** The screen is shown: what is chosen is read. */
    fun onLoad()

    fun onBack()

    /** A mode tapped in "Mode": kept at once. */
    fun onSelectTheme(mode: ThemeMode)

    /** A language picked in "Language": kept at once. */
    fun onSelectLanguage(language: AppLanguage)

    /** "Password coloring" switched: kept at once. */
    fun onSetPasswordColoring(on: Boolean)

    /** "Password strength" switched: kept at once. */
    fun onSetPasswordStrength(on: Boolean)

    /** "User name in the list" switched: kept at once. */
    fun onSetUserNameInList(on: Boolean)

    /** "Number of entries in a group" switched: kept at once. */
    fun onSetEntryCount(on: Boolean)

    /** "Count nested groups" switched: kept at once. */
    fun onSetEntryCountNested(on: Boolean)

    /** The snackbar took the message. */
    fun onMessageShown()
}
