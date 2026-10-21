package ru.kino.dev.settings.pres.presets

import ru.kino.dev.core.preset.EntryPreset
import ru.kino.dev.core.preset.PresetId

sealed interface PresetsAction {

    /** The screen is shown - first or back from a preset: what is chosen and the presets are read. */
    data object Load : PresetsAction

    data object Back : PresetsAction

    /** "Use presets" switched: kept at once. */
    data class SetUsePresets(val on: Boolean) : PresetsAction

    /** The fab "Create": a new preset, while there is room for one. */
    data object Create : PresetsAction

    /** "Edit" in the menu of the preset of [id]. */
    data class Edit(val id: PresetId) : PresetsAction

    /** "Delete" in the menu of [preset]: asks first. */
    data class Delete(val preset: EntryPreset) : PresetsAction

    data object DeleteConfirm : PresetsAction

    data object DeleteDismiss : PresetsAction

    /** The snackbar took the message. */
    data object MessageShown : PresetsAction
}

/** What the snackbar says. */
enum class PresetsMessage {
    /** The choice could not be written: it shows what is kept. */
    NotSaved,

    /** The preset could not be deleted - the store could not be written; it stays. */
    NotDeleted,
}
