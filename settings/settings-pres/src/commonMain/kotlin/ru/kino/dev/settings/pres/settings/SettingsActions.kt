package ru.kino.dev.settings.pres.settings

/** What the main settings screen is asked to do - done by [SettingsProcessor] (pass-docs, `docs/app/mvi-arc.md`). */
interface SettingsActions {

    /** "Back" in the top bar. */
    fun onBack()

    /** A tap on a card: the screen behind it, when it is made (pass-docs, Step 55). */
    fun onOpen(item: SettingsItem)

    /** A tap on "About" - its screen (pass-docs, Step 122). */
    fun onOpenAbout()
}
