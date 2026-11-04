package ru.kino.dev.settings.pres.presetscontainer

/** What the container of the presets asks of its screen once. */
sealed interface PresetsContainerSideEffect {

    /** Closing the pane [pane] - asked of its form: with changes it asks first (pass-docs, Step 100, item 3). */
    data class ClosePane(val pane: String) : PresetsContainerSideEffect
}
