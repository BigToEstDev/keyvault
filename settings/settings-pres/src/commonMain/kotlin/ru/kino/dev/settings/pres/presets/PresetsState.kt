package ru.kino.dev.settings.pres.presets

import ru.kino.dev.core.preset.EntryPreset
import ru.kino.dev.core.preset.PresetValidator

/**
 * @param usePresets whether presets are used; null until read - its choice is disabled
 * @param presets the presets of one's own, in the order they were made; null until read
 * @param deleting "Delete the preset?" is open for it
 * @param message for the snackbar, until it takes it
 * @param drafts the new presets open beside the list, in a container, not saved yet - each takes a place as one kept
 *   (pass-docs, Step 99, item 7); on a phone - none
 */
data class PresetsState(
    val usePresets: Boolean? = null,
    val presets: List<EntryPreset>? = null,
    val deleting: EntryPreset? = null,
    val message: PresetsMessage? = null,
    val drafts: Int = 0,
) {
    /** One more preset can be made - the most a person has is not reached, [drafts] counted (pass-docs, Step 44). */
    val canCreate: Boolean = presets?.let { PresetValidator.canAdd(it.size + drafts) } ?: false
}

/** What the snackbar says. */
enum class PresetsMessage {
    /** The choice could not be written: it shows what is kept. */
    NotSaved,

    /** The preset could not be deleted - the store could not be written; it stays. */
    NotDeleted,
}
