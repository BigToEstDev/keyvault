package ru.kino.dev.settings.pres.privacy

sealed interface PrivacyAction {

    /** The screen is shown: what is chosen is read. */
    data object Load : PrivacyAction

    data object Back : PrivacyAction

    /** "Allow screenshots" switched: kept at once. */
    data class SetAllowScreenshots(val on: Boolean) : PrivacyAction

    /** The snackbar took the message. */
    data object MessageShown : PrivacyAction
}

/** What the snackbar says. */
enum class PrivacyMessage {
    /** The choice could not be written: it shows what is kept. */
    NotSaved,
}
