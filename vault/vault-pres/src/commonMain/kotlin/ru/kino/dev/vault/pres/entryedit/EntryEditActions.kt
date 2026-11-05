package ru.kino.dev.vault.pres.entryedit

import ru.kino.dev.core.FieldType
import ru.kino.dev.core.preset.PresetId
import ru.kino.dev.vault.group.GroupId

/** What the form of an entry is asked to do - done by [EntryEditProcessor] (pass-docs, `docs/app/mvi-arc.md`). */
interface EntryEditActions {

    /** A value of a field of the preset, under its [key]. */
    fun onValueChange(key: String, value: String)

    /** A value of the own field at [index]. */
    fun onOwnValueChange(index: Int, value: String)

    fun onTogglePassword()

    /** Shows or hides the value of the protected own field at [index]. */
    fun onToggleOwn(index: Int)

    /** Shows or hides the value of the field of the preset under [key], private by the database. */
    fun onToggleKey(key: String)

    /** Another group picked for the entry - it goes there on saving. */
    fun onGroupChosen(id: GroupId)

    /** The preset button: the choice of presets, read anew. */
    fun onChoosePreset()

    /** A preset picked in the choice: the one of [id], or the standard one for null. */
    fun onPresetChosen(id: PresetId?)

    /** "+ Create preset" in the choice: the screen of a new preset. */
    fun onCreatePreset()

    /** The screen is shown - again, maybe, after a preset was made: that one is picked. */
    fun onResumed()

    /** "+ Add field": asks for the name of a new own field, whether it is protected and its type. */
    fun onAddField()

    /** "Edit" in the menu of the own field at [index]: its name, whether it is protected and its type. */
    fun onEditField(index: Int)

    fun onDeleteField(index: Int)

    fun onDialogNameChange(name: String)

    /** The switch "protected field" of the dialog of a field. */
    fun onDialogProtectedChange(protected: Boolean)

    /** The type picked in the dialog of a field. */
    fun onDialogTypeChange(type: FieldType)

    /** The digits of the phone field [target] typed - after the calling code (pass-docs, Step 77). */
    fun onPhoneDigitsChange(target: PhoneTarget, digits: String)

    /** The prefix of the phone field [target] tapped: asks for the country. */
    fun onChoosePhoneCountry(target: PhoneTarget)

    /** The country picked in [EntryEditDialog.PhoneCountries] - its code. */
    fun onPhoneCountryChosen(region: String)

    fun onDialogConfirm()

    fun onDialogDismiss()

    /** "Add outside the preset" in [EntryEditDialog.HiddenFields]: the hidden fields as own ones, then the save. */
    fun onKeepHidden()

    /** "+ Add attachment": asks for the file to attach (pass-docs, Step 103). */
    fun onAddAttachment()

    /** The file to attach, picked - [uri]: read, within the limit, under a name the form has not. */
    fun onAttachmentPicked(uri: String)

    /** The cross by the attachment at [index]: off the form, unasked - nothing is lost until "Save". */
    fun onRemoveAttachment(index: Int)

    /** The fab: checks, makes the change and writes the file. */
    fun onSave()

    /** "Back" - in the top bar or the system one. */
    fun onBack()

    /** The cross of the tab of the form in a container: as "back", but the tab closes (pass-docs, Step 98, item 8). */
    fun onClose()

    /** "Delete" in the menu of the top bar - an entry that is there, not a new one: asks first. */
    fun onDelete()

    /** The snackbar took the message. */
    fun onMessageShown()
}
