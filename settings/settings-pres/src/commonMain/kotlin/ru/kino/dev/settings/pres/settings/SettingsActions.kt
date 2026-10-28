package ru.kino.dev.settings.pres.settings

sealed interface SettingsAction {

    /** "Back" in the top bar. */
    data object Back : SettingsAction

    /** A tap on a card: the screen behind it, when it is made (pass-docs, Step 55). */
    data class Open(val item: SettingsItem) : SettingsAction
}
