package ru.kino.dev.vault.pres.presetcreate

import ru.kino.dev.core.preset.FieldNameProblem
import ru.kino.dev.core.preset.FieldPosition

sealed interface PresetCreateAction {

    /** The name of the preset, typed. */
    data class NameChanged(val name: String) : PresetCreateAction

    /** "+ Add field" of the card [card]: asks for the name. */
    data class AddField(val card: Int) : PresetCreateAction

    data class RenameField(val at: FieldPosition) : PresetCreateAction

    data class DeleteField(val at: FieldPosition) : PresetCreateAction

    /** "Add card": a new empty card, last before the notes. */
    data object AddCard : PresetCreateAction

    /** The red cross of the card [card] - one the person added: asks first. */
    data class DeleteCard(val card: Int) : PresetCreateAction

    data class DialogNameChanged(val name: String) : PresetCreateAction

    data object DialogConfirm : PresetCreateAction

    data object DialogDismiss : PresetCreateAction

    /**
     * The fab: checks, makes the preset and leaves. [standardName] is what the screens call the standard
     * preset - no other may be called so; it comes from the resources, which the processor does not see.
     */
    data class Save(val standardName: String) : PresetCreateAction

    /** "Back" - in the top bar or the system one. */
    data object Back : PresetCreateAction

    /** The snackbar took the message. */
    data object MessageShown : PresetCreateAction
}

/** The dialog over the form, if any. */
sealed interface PresetCreateDialog {

    /** The name of a new field of the card [card]. */
    data class AddField(val card: Int, val name: String = "", val problem: FieldNameProblem? = null) : PresetCreateDialog

    /** A new name for the field [at]. */
    data class RenameField(val at: FieldPosition, val name: String, val problem: FieldNameProblem? = null) : PresetCreateDialog

    /** "Delete the field?" - the field [at], called [name]. */
    data class DeleteField(val at: FieldPosition, val name: String) : PresetCreateDialog

    /** "Delete the card? Its fields go too." */
    data class DeleteCard(val card: Int) : PresetCreateDialog

    /** "Leave without saving?" - "back" with changes. */
    data object Leave : PresetCreateDialog
}

/** What the snackbar says. */
enum class PresetCreateMessage {
    /** There are ten presets already, the standard one included */
    TooMany,

    /** The store could not be written; nothing is made, the form stays */
    NotSaved,
}
