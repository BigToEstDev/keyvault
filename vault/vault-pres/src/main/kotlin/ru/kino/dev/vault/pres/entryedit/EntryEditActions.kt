package ru.kino.dev.vault.pres.entryedit

import ru.kino.dev.vault.entry.NameProblem

sealed interface EntryEditAction {

    /** A value of a field of the preset, under its [key]. */
    data class ValueChanged(val key: String, val value: String) : EntryEditAction

    /** A value of the own field at [index]. */
    data class OwnValueChanged(val index: Int, val value: String) : EntryEditAction

    data object TogglePassword : EntryEditAction

    /** Shows or hides the value of the protected own field at [index]. */
    data class ToggleOwn(val index: Int) : EntryEditAction

    /** The preset button: the choice of presets - empty until presets of one's own come (Step 44). */
    data object ChoosePreset : EntryEditAction

    /** "+ Add field": asks for the name of a new own field. */
    data object AddField : EntryEditAction

    data class RenameField(val index: Int) : EntryEditAction

    data class DeleteField(val index: Int) : EntryEditAction

    data class DialogNameChanged(val name: String) : EntryEditAction

    data object DialogConfirm : EntryEditAction

    data object DialogDismiss : EntryEditAction

    /** The fab: checks, makes the change and writes the file. */
    data object Save : EntryEditAction

    /** "Back" - in the top bar or the system one. */
    data object Back : EntryEditAction

    /** The snackbar took the message. */
    data object MessageShown : EntryEditAction
}

/** The dialog over the form, if any. */
sealed interface EntryEditDialog {

    /** The name of a new own field. */
    data class AddField(val name: String = "", val problem: NameProblem? = null) : EntryEditDialog

    /** A new name for the own field at [index]. */
    data class RenameField(val index: Int, val name: String, val problem: NameProblem? = null) : EntryEditDialog

    /** "Delete the field?" - the own field at [index], called [name]. */
    data class DeleteField(val index: Int, val name: String) : EntryEditDialog

    /** The choice of presets - empty for now, closes and changes nothing. */
    data object Preset : EntryEditDialog

    /** "Leave without saving?" - "back" with changes. */
    data object Leave : EntryEditDialog

    /** The change is made but the file is not written; the screen closes after it. */
    data object NotSaved : EntryEditDialog
}

/** What the snackbar says. */
enum class EntryEditMessage { LoadFailed, Unexpected }
