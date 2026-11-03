package ru.kino.dev.vault.pres.container

/** What the container of `vault` asks of its screen once. */
sealed interface VaultContainerSideEffect {

    /** "Back" of the pane [pane], as its screen would go back: the cross of a tab with a form (pass-docs, Step 98, item 6). */
    data class PaneBack(val pane: String) : VaultContainerSideEffect
}
