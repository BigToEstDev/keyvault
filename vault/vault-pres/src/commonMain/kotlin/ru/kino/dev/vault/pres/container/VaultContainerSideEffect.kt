package ru.kino.dev.vault.pres.container

/** What the container of `vault` asks of its screen once. */
sealed interface VaultContainerSideEffect {

    /** Closing the pane [pane] - asked of its screen: the cross of a tab with a form (pass-docs, Step 98, item 8). */
    data class ClosePane(val pane: String) : VaultContainerSideEffect
}
