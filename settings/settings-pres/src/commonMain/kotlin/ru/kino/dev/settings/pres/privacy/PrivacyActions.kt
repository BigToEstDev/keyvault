package ru.kino.dev.settings.pres.privacy

/** What the privacy settings is asked to do - done by [PrivacyProcessor] (pass-docs, `docs/app/mvi-arc.md`). */
interface PrivacyActions {

    /** The screen is shown: what is chosen is read. */
    fun onLoad()

    fun onBack()

    /** "Allow screenshots" switched: kept at once. */
    fun onSetAllowScreenshots(on: Boolean)

    /** The snackbar took the message. */
    fun onMessageShown()
}
