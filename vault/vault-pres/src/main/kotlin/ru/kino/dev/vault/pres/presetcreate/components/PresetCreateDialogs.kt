package ru.kino.dev.vault.pres.presetcreate.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ru.kino.dev.core.pres.R
import ru.kino.dev.core.preset.FieldNameProblem
import ru.kino.dev.core.preset.PresetNameProblem
import ru.kino.dev.core.preset.PresetLimits
import ru.kino.dev.ui.components.ConfirmDialog
import ru.kino.dev.ui.components.TextInputDialog
import ru.kino.dev.vault.pres.presetcreate.PresetCreateDialog

/** The dialog over the form of a preset: the name of a field, "delete the field?", "delete the card?", "leave?". */
@Composable
internal fun PresetCreateDialogs(
    dialog: PresetCreateDialog?,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    when (dialog) {
        null -> Unit

        is PresetCreateDialog.AddField -> TextInputDialog(
            title = stringResource(R.string.entry_add_field_title),
            value = dialog.name,
            onValueChange = onNameChange,
            label = stringResource(R.string.entry_field_name),
            confirmText = stringResource(R.string.entry_add_field_confirm),
            dismissText = stringResource(R.string.action_cancel),
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            error = dialog.problem?.let { fieldNameProblemText(it) },
        )

        is PresetCreateDialog.RenameField -> TextInputDialog(
            title = stringResource(R.string.entry_rename_field_title),
            value = dialog.name,
            onValueChange = onNameChange,
            label = stringResource(R.string.entry_field_name),
            confirmText = stringResource(R.string.action_save),
            dismissText = stringResource(R.string.action_cancel),
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            error = dialog.problem?.let { fieldNameProblemText(it) },
        )

        is PresetCreateDialog.DeleteField -> ConfirmDialog(
            title = stringResource(R.string.entry_delete_field_title),
            text = stringResource(R.string.entry_delete_field_text, dialog.name),
            confirmText = stringResource(R.string.entry_field_delete),
            dismissText = stringResource(R.string.action_cancel),
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            destructive = true,
        )

        is PresetCreateDialog.DeleteCard -> ConfirmDialog(
            title = stringResource(R.string.preset_delete_card_title),
            text = stringResource(R.string.preset_delete_card_text),
            confirmText = stringResource(R.string.entry_field_delete),
            dismissText = stringResource(R.string.action_cancel),
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            destructive = true,
        )

        PresetCreateDialog.Leave -> ConfirmDialog(
            title = stringResource(R.string.entry_leave_title),
            text = stringResource(R.string.entry_leave_text),
            confirmText = stringResource(R.string.entry_leave_confirm),
            dismissText = stringResource(R.string.entry_leave_stay),
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )
    }
}

/** Why the name of a field of a preset cannot be used - in the dialog and under the field. */
@Composable
internal fun fieldNameProblemText(problem: FieldNameProblem): String = when (problem) {
    FieldNameProblem.EMPTY -> stringResource(R.string.entry_field_name_empty)
    FieldNameProblem.TAKEN -> stringResource(R.string.entry_field_name_taken)
}

/** Why the name of a preset cannot be used - under the field. */
@Composable
internal fun presetNameProblemText(problem: PresetNameProblem): String = when (problem) {
    PresetNameProblem.EMPTY -> stringResource(R.string.preset_name_empty)
    PresetNameProblem.TOO_LONG -> stringResource(R.string.preset_name_too_long, PresetLimits.MAX_NAME_LENGTH)
    PresetNameProblem.TAKEN -> stringResource(R.string.preset_name_taken)
}
