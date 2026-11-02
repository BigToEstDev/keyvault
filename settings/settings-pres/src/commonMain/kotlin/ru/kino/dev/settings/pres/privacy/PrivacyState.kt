package ru.kino.dev.settings.pres.privacy

/**
 * @param allowScreenshots whether the screen of the app may be captured; null until read - its choice is disabled
 * @param message for the snackbar, until it takes it
 */
data class PrivacyState(
    val allowScreenshots: Boolean? = null,
    val message: PrivacyMessage? = null,
)

/** What the snackbar says. */
enum class PrivacyMessage {
    /** The choice could not be written: it shows what is kept. */
    NotSaved,
}
