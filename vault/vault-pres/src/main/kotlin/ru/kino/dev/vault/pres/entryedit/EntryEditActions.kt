package ru.kino.dev.vault.pres.entryedit

import ru.kino.dev.core.preset.EntryPreset
import ru.kino.dev.core.preset.PresetId
import ru.kino.dev.vault.entry.NameProblem
import ru.kino.dev.vault.group.GroupId

sealed interface EntryEditAction {

    /** A value of a field of the preset, under its [key]. */
    data class ValueChanged(val key: String, val value: String) : EntryEditAction

    /** A value of the own field at [index]. */
    data class OwnValueChanged(val index: Int, val value: String) : EntryEditAction

    data object TogglePassword : EntryEditAction

    /** Shows or hides the value of the protected own field at [index]. */
    data class ToggleOwn(val index: Int) : EntryEditAction

    /** Another group picked for the entry - it goes there on saving. */
    data class GroupChosen(val id: GroupId) : EntryEditAction

    /** The preset button: the choice of presets, read anew. */
    data object ChoosePreset : EntryEditAction

    /** A preset picked in the choice: the one of [id], or the standard one for null. */
    data class PresetChosen(val id: PresetId?) : EntryEditAction

    /** "+ Add field": asks for the name of a new own field. */
    data object AddField : EntryEditAction

    data class RenameField(val index: Int) : EntryEditAction

    data class DeleteField(val index: Int) : EntryEditAction

    data class DialogNameChanged(val name: String) : EntryEditAction

    data object DialogConfirm : EntryEditAction

    data object DialogDismiss : EntryEditAction

    /** "Add outside the preset" in [EntryEditDialog.HiddenFields]: the hidden fields as own ones, then the save. */
    data object KeepHidden : EntryEditAction

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

    /**
     * The choice of presets: the standard one, then [presets] - those of one's own, in the order they were
     * made; [selected] is the one of the form, null for the standard one.
     */
    data class Preset(val presets: List<EntryPreset>, val selected: PresetId?) : EntryEditDialog

    /**
     * "Save" with fields hidden by the preset chosen - their [keys]: "save as is" (confirm) drops them, "add
     * outside the preset" ([EntryEditAction.KeepHidden]) keeps them as own fields, "cancel" stays.
     */
    data class HiddenFields(val keys: List<String>) : EntryEditDialog

    /** "Leave without saving?" - "back" with changes. */
    data object Leave : EntryEditDialog

    /** The change is made but the file is not written; the screen closes after it. */
    data object NotSaved : EntryEditDialog
}

/** What the snackbar says. */
enum class EntryEditMessage { LoadFailed, Unexpected }
