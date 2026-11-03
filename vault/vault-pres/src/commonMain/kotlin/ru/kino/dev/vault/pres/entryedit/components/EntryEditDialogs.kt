package ru.kino.dev.vault.pres.entryedit.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import ru.kino.dev.core.FieldType
import ru.kino.dev.core.pres.*
import ru.kino.dev.core.preset.PresetId
import ru.kino.dev.ui.components.ConfirmDialog
import ru.kino.dev.ui.components.FieldDialog
import ru.kino.dev.ui.phone.PhoneCountryDialog
import ru.kino.dev.ui.res.stringResource
import ru.kino.dev.vault.entry.NameProblem
import ru.kino.dev.vault.pres.common.EntryDeleteDialog
import ru.kino.dev.vault.pres.entryedit.EntryEditDialog

/**
 * The dialog over the form: an own field - its name and protection, "delete the field?", the presets, fields
 * outside the preset, "leave?", "not written".
 */
@Composable
internal fun EntryEditDialogs(
    dialog: EntryEditDialog?,
    onNameChange: (String) -> Unit,
    onProtectedChange: (Boolean) -> Unit,
    onTypeChange: (FieldType) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onPresetChosen: (PresetId?) -> Unit,
    onCreatePreset: () -> Unit,
    onKeepHidden: () -> Unit,
    onPhoneCountryChosen: (String) -> Unit,
) {
    when (dialog) {
        null -> Unit

        is EntryEditDialog.AddField -> FieldDialog(
            title = stringResource(Res.string.entry_add_field_title),
            name = dialog.name,
            onNameChange = onNameChange,
            protected = dialog.protected,
            onProtectedChange = onProtectedChange,
            type = FieldType.of(dialog.type),
            onTypeChange = onTypeChange,
            confirmText = stringResource(Res.string.action_add),
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            error = dialog.problem?.let { nameProblemText(it) },
        )

        is EntryEditDialog.EditField -> FieldDialog(
            title = stringResource(Res.string.entry_edit_field_title),
            name = dialog.name,
            onNameChange = onNameChange,
            protected = dialog.protected,
            onProtectedChange = onProtectedChange,
            type = FieldType.of(dialog.type),
            onTypeChange = onTypeChange,
            confirmText = stringResource(Res.string.action_save),
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            error = dialog.problem?.let { nameProblemText(it) },
        )

        is EntryEditDialog.DeleteField -> ConfirmDialog(
            title = stringResource(Res.string.entry_delete_field_title),
            text = stringResource(Res.string.entry_delete_field_text, dialog.name),
            confirmText = stringResource(Res.string.action_delete),
            dismissText = stringResource(Res.string.action_cancel),
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            destructive = true,
        )

        is EntryEditDialog.PhoneCountries -> PhoneCountryDialog(
            countries = dialog.countries,
            selected = dialog.selected,
            onChoose = onPhoneCountryChosen,
            onClose = onDismiss,
        )

        is EntryEditDialog.Preset -> PresetChoiceDialog(
            title = stringResource(Res.string.entry_preset),
            presets = dialog.presets,
            selected = dialog.selected,
            onChoose = onPresetChosen,
            onCreate = onCreatePreset,
            onClose = onDismiss,
        )

        is EntryEditDialog.HiddenFields -> HiddenFieldsDialog(
            keys = dialog.keys,
            onSaveAsIs = onConfirm,
            onKeep = onKeepHidden,
            onCancel = onDismiss,
        )

        is EntryEditDialog.Delete -> EntryDeleteDialog(
            title = dialog.title,
            toBin = dialog.toBin,
            deleting = dialog.deleting,
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )

        is EntryEditDialog.Leave -> ConfirmDialog(
            title = stringResource(Res.string.entry_leave_title),
            text = stringResource(Res.string.entry_leave_text),
            confirmText = stringResource(Res.string.entry_leave_confirm),
            dismissText = stringResource(Res.string.entry_leave_stay),
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )

        // One button: the change is made either way, there is nothing to choose
        EntryEditDialog.NotSaved -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(Res.string.entry_not_saved_title)) },
            text = { Text(stringResource(Res.string.entry_not_saved_text)) },
            confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(Res.string.action_ok)) } },
        )
    }
}

/** Why the name of an own field cannot be used - under the field in a dialog and on the form. */
@Composable
internal fun nameProblemText(problem: NameProblem): String = when (problem) {
    NameProblem.EMPTY -> stringResource(Res.string.error_name_empty)
    NameProblem.TAKEN -> stringResource(Res.string.entry_field_name_taken)
}
