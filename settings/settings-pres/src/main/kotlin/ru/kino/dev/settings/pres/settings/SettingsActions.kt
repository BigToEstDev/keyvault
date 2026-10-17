package ru.kino.dev.settings.pres.settings

sealed interface SettingsAction {

    /** "Back" in the top bar. */
    data object Back : SettingsAction
}
