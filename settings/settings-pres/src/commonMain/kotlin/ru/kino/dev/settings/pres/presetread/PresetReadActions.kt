package ru.kino.dev.settings.pres.presetread

/** What the reading of a preset is asked to do - done by [PresetReadProcessor] (pass-docs, `docs/app/mvi-arc.md`). */
interface PresetReadActions {

    /** The screen is shown: the preset is read; gone - back. */
    fun onLoad()

    /** "Back" - in the top bar or the system one. */
    fun onBack()

    /** The fab "Edit" - one's own: its form. */
    fun onEdit()

    /** "Delete" in the menu - one's own: asks first. */
    fun onDelete()

    fun onDeleteConfirm()

    fun onDeleteDismiss()

    /** The snackbar took the message. */
    fun onMessageShown()
}
