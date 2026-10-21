package ru.kino.dev.vault.pres.entryedit.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ru.kino.dev.core.pres.R
import ru.kino.dev.core.preset.PresetId
import ru.kino.dev.ui.components.ConfirmDialog
import ru.kino.dev.vault.pres.common.EntryDeleteDialog
import ru.kino.dev.ui.components.TextInputDialog
import ru.kino.dev.vault.entry.NameProblem
import ru.kino.dev.vault.pres.entryedit.EntryEditDialog

/**
 * The dialog over the form: the name of an own field, "delete the field?", the presets, fields outside the
 * preset, "leave?", "not written".
 */
@Composable
internal fun EntryEditDialogs(
    dialog: EntryEditDialog?,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onPresetChosen: (PresetId?) -> Unit,
    onCreatePreset: () -> Unit,
    onKeepHidden: () -> Unit,
) {
    when (dialog) {
        null -> Unit

        is EntryEditDialog.AddField -> TextInputDialog(
            title = stringResource(R.string.entry_add_field_title),
            value = dialog.name,
            onValueChange = onNameChange,
            label = stringResource(R.string.entry_field_name),
            confirmText = stringResource(R.string.action_add),
            dismissText = stringResource(R.string.action_cancel),
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            error = dialog.problem?.let { nameProblemText(it) },
        )

        is EntryEditDialog.RenameField -> TextInputDialog(
            title = stringResource(R.string.entry_rename_field_title),
            value = dialog.name,
            onValueChange = onNameChange,
            label = stringResource(R.string.entry_field_name),
            confirmText = stringResource(R.string.action_save),
            dismissText = stringResource(R.string.action_cancel),
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            error = dialog.problem?.let { nameProblemText(it) },
        )

        is EntryEditDialog.DeleteField -> ConfirmDialog(
            title = stringResource(R.string.entry_delete_field_title),
            text = stringResource(R.string.entry_delete_field_text, dialog.name),
            confirmText = stringResource(R.string.action_delete),
            dismissText = stringResource(R.string.action_cancel),
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            destructive = true,
        )

        is EntryEditDialog.Preset -> PresetChoiceDialog(
            title = stringResource(R.string.entry_preset),
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
            deleting = dialog.deleting,
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )

        EntryEditDialog.Leave -> ConfirmDialog(
            title = stringResource(R.string.entry_leave_title),
            text = stringResource(R.string.entry_leave_text),
            confirmText = stringResource(R.string.entry_leave_confirm),
            dismissText = stringResource(R.string.entry_leave_stay),
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )

        // One button: the change is made either way, there is nothing to choose
        EntryEditDialog.NotSaved -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.entry_not_saved_title)) },
            text = { Text(stringResource(R.string.entry_not_saved_text)) },
            confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.action_ok)) } },
        )
    }
}

/** Why the name of an own field cannot be used - under the field in a dialog and on the form. */
@Composable
internal fun nameProblemText(problem: NameProblem): String = when (problem) {
    NameProblem.EMPTY -> stringResource(R.string.error_name_empty)
    NameProblem.TAKEN -> stringResource(R.string.entry_field_name_taken)
}
