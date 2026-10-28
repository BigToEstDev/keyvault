package ru.kino.dev.settings.pres.presetedit

import ru.kino.dev.core.FieldType
import ru.kino.dev.core.preset.FieldNameProblem
import ru.kino.dev.core.preset.FieldPosition

sealed interface PresetEditAction {

    /** The screen is shown: the preset to edit is read; a new one has nothing to read. */
    data object Load : PresetEditAction

    /** The name of the preset, typed. */
    data class NameChanged(val name: String) : PresetEditAction

    /** "+ Add field" of the card [card]: asks for the name, whether it is protected and the type. */
    data class AddField(val card: Int) : PresetEditAction

    /** "Edit" in the menu of the field [at]: its name, whether it is protected and its type. */
    data class EditField(val at: FieldPosition) : PresetEditAction

    data class DeleteField(val at: FieldPosition) : PresetEditAction

    /** "Add card": a new empty card, last before the notes. */
    data object AddCard : PresetEditAction

    /** The red cross of the card [card] - one the person added: asks first. */
    data class DeleteCard(val card: Int) : PresetEditAction

    data class DialogNameChanged(val name: String) : PresetEditAction

    /** The switch "protected field" of the dialog of a field. */
    data class DialogProtectedChanged(val protected: Boolean) : PresetEditAction

    /** The type picked in the dialog of a field. */
    data class DialogTypeChanged(val type: FieldType) : PresetEditAction

    data object DialogConfirm : PresetEditAction

    data object DialogDismiss : PresetEditAction

    /**
     * The fab: checks, makes or changes the preset and leaves - a change that drops fields of the preset asks first. [standardName] is what the screens call the standard
     * preset - no other may be called so; it comes from the resources, which the processor does not see.
     */
    data class Save(val standardName: String) : PresetEditAction

    /** "Back" - in the top bar or the system one. */
    data object Back : PresetEditAction

    /** The snackbar took the message. */
    data object MessageShown : PresetEditAction
}

/** The dialog over the form, if any. */
sealed interface PresetEditDialog {

    /**
     * A new field of the card [card]: its name, whether it is protected - not, until switched - and its [type] -
     * text, until picked (Step 75).
     */
    data class AddField(
        val card: Int,
        val name: String = "",
        val protected: Boolean = false,
        val type: String? = null,
        val problem: FieldNameProblem? = null,
    ) : PresetEditDialog

    /** The field [at] changed: its name, whether it is protected and its [type], opened as they are. */
    data class EditField(
        val at: FieldPosition,
        val name: String,
        val protected: Boolean,
        val type: String?,
        val problem: FieldNameProblem? = null,
    ) : PresetEditDialog

    /** "Delete the field?" - the field [at], called [name]. */
    data class DeleteField(val at: FieldPosition, val name: String) : PresetEditDialog

    /** "Delete the card? Its fields go too." */
    data class DeleteCard(val card: Int) : PresetEditDialog

    /** "Leave without saving?" - "back" with changes. */
    data object Leave : PresetEditDialog

    /**
     * "Entries with this preset will show the old fields as their own" - a change of a preset that drops names of
     * its fields: renamed, deleted, gone with a card (pass-docs, Step 62). [standardName] - for the save it leads to.
     */
    data class FieldsDropped(val standardName: String) : PresetEditDialog
}

/** What the snackbar says. */
enum class PresetEditMessage {
    /** There are ten presets already, the standard one included */
    TooMany,

    /** The store could not be written; nothing is made, the form stays */
    NotSaved,

    /** The preset edited is gone - deleted on another screen; nothing is saved */
    Gone,
}
