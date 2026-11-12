package ru.kino.dev.settings.pres.about

/** What the screen "About" does outside the app: the browser, the clipboard. */
sealed interface AboutSideEffect {

    /** [link] opened in the browser of the system. */
    data class OpenLink(val link: String) : AboutSideEffect

    /** [email] put into the clipboard. */
    data class CopyEmail(val email: String) : AboutSideEffect
}
