package ru.kino.dev.settings.pres.about

/** What the screen "About" is asked to do - done by [AboutProcessor] (pass-docs, `docs/app/mvi-arc.md`). */
interface AboutActions {

    /** The screen is shown: the version of the core is asked (pass-docs, Step 123). */
    fun onLoad()

    /** "Back" in the top bar. */
    fun onBack()

    /** "Support the project": no page to lead to yet - nothing happens (pass-docs, Step 122). */
    fun onSupport()

    /** The privacy policy: its page on the site is not made yet - nothing happens (pass-docs, Step 122). */
    fun onOpenPrivacyPolicy()

    /** The site: not published yet - nothing happens (pass-docs, Step 122). */
    fun onOpenSite()

    /** The address to write to - copied. */
    fun onCopyEmail()

    /** "Report a bug" - the issues of the repository, in the browser. */
    fun onReportIssue()

    /** The source code - the repository of the app, in the browser. */
    fun onOpenSourceCode()
}
