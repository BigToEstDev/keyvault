package ru.kino.dev.settings.pres.presetread

import ru.kino.dev.core.preset.EntryPreset

/**
 * A preset read (pass-docs, Step 99, item 8).
 *
 * @param preset the preset as kept; null until read
 * @param deleting "Delete the preset?" is open
 * @param working the preset is being deleted - the menu and the fab wait
 * @param message for the snackbar, until it takes it
 */
data class PresetReadState(
    val preset: EntryPreset? = null,
    val deleting: Boolean = false,
    val working: Boolean = false,
    val message: PresetReadMessage? = null,
) {
    /** One's own: it is edited and deleted here; the standard one is only read. */
    val own: Boolean = preset?.isStandard == false
}

/** What the snackbar says. */
enum class PresetReadMessage {
    /** The preset could not be deleted - the store could not be written; it stays. */
    NotDeleted,
}
