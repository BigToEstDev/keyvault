package ru.kino.dev.settings.pres.presets

import ru.kino.dev.core.preset.EntryPreset
import ru.kino.dev.core.preset.PresetId

/** What the list of presets is asked to do - done by [PresetsProcessor] (pass-docs, `docs/app/mvi-arc.md`). */
interface PresetsActions {

    /** The screen is shown - first or back from a preset: what is chosen and the presets are read. */
    fun onLoad()

    fun onBack()

    /** "Use presets" switched: kept at once. */
    fun onSetUsePresets(on: Boolean)

    /** The fab "Create": a new preset, while there is room for one. */
    fun onCreate()

    /** "Edit" in the menu of the preset of [id]. */
    fun onEdit(id: PresetId)

    /** "Delete" in the menu of [preset]: asks first. */
    fun onDelete(preset: EntryPreset)

    fun onDeleteConfirm()

    fun onDeleteDismiss()

    /** The snackbar took the message. */
    fun onMessageShown()
}
