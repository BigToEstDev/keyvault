package ru.kino.dev.settings.pres.presetedit

import ru.kino.dev.core.FieldType
import ru.kino.dev.core.preset.FieldPosition

/** What the form of a preset is asked to do - done by [PresetEditProcessor] (pass-docs, `docs/app/mvi-arc.md`). */
interface PresetEditActions {

    /** The screen is shown: the preset to edit is read; a new one has nothing to read. */
    fun onLoad()

    /** The name of the preset, typed. */
    fun onNameChange(name: String)

    /** "+ Add field" of the card [card]: asks for the name, whether it is protected and the type. */
    fun onAddField(card: Int)

    /** "Edit" in the menu of the field [at]: its name, whether it is protected and its type. */
    fun onEditField(at: FieldPosition)

    fun onDeleteField(at: FieldPosition)

    /** "Add card": a new empty card, last before the notes. */
    fun onAddCard()

    /** The red cross of the card [card] - one the person added: asks first. */
    fun onDeleteCard(card: Int)

    fun onDialogNameChange(name: String)

    /** The switch "protected field" of the dialog of a field. */
    fun onDialogProtectedChange(protected: Boolean)

    /** The type picked in the dialog of a field. */
    fun onDialogTypeChange(type: FieldType)

    fun onDialogConfirm()

    fun onDialogDismiss()

    /**
     * The fab: checks, makes or changes the preset and leaves - a change that drops fields of the preset asks first. [standardName] is what the screens call the standard
     * preset - no other may be called so; it comes from the resources, which the processor does not see.
     */
    fun onSave(standardName: String)

    /** "Back" - in the top bar or the system one. */
    fun onBack()

    /** The snackbar took the message. */
    fun onMessageShown()
}
