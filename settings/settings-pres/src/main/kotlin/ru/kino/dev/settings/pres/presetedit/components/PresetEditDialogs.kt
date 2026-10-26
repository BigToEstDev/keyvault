package ru.kino.dev.settings.pres.presetedit.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import ru.kino.dev.core.FieldType
import ru.kino.dev.core.pres.R
import ru.kino.dev.core.preset.FieldNameProblem
import ru.kino.dev.core.preset.PresetLimits
import ru.kino.dev.core.preset.PresetNameProblem
import ru.kino.dev.settings.pres.presetedit.PresetEditDialog
import ru.kino.dev.ui.components.ConfirmDialog
import ru.kino.dev.ui.components.FieldDialog

/**
 * The dialog over the form of a preset: a field - its name and protection, "delete the field?", "delete the card?", "leave?",
 * "entries will show the old fields as their own".
 */
@Composable
internal fun PresetEditDialogs(
    dialog: PresetEditDialog?,
    onNameChange: (String) -> Unit,
    onProtectedChange: (Boolean) -> Unit,
    onTypeChange: (FieldType) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    when (dialog) {
        null -> Unit

        is PresetEditDialog.AddField -> FieldDialog(
            title = stringResource(R.string.entry_add_field_title),
            name = dialog.name,
            onNameChange = onNameChange,
            protected = dialog.protected,
            onProtectedChange = onProtectedChange,
            type = FieldType.of(dialog.type),
            onTypeChange = onTypeChange,
            confirmText = stringResource(R.string.action_add),
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            error = dialog.problem?.let { fieldNameProblemText(it) },
        )

        is PresetEditDialog.EditField -> FieldDialog(
            title = stringResource(R.string.entry_edit_field_title),
            name = dialog.name,
            onNameChange = onNameChange,
            protected = dialog.protected,
            onProtectedChange = onProtectedChange,
            type = FieldType.of(dialog.type),
            onTypeChange = onTypeChange,
            confirmText = stringResource(R.string.action_save),
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            error = dialog.problem?.let { fieldNameProblemText(it) },
        )

        is PresetEditDialog.DeleteField -> ConfirmDialog(
            title = stringResource(R.string.entry_delete_field_title),
            text = stringResource(R.string.entry_delete_field_text, dialog.name),
            confirmText = stringResource(R.string.action_delete),
            dismissText = stringResource(R.string.action_cancel),
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            destructive = true,
        )

        is PresetEditDialog.DeleteCard -> ConfirmDialog(
            title = stringResource(R.string.preset_delete_card_title),
            text = stringResource(R.string.preset_delete_card_text),
            confirmText = stringResource(R.string.action_delete),
            dismissText = stringResource(R.string.action_cancel),
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            destructive = true,
        )

        PresetEditDialog.Leave -> ConfirmDialog(
            title = stringResource(R.string.entry_leave_title),
            text = stringResource(R.string.entry_leave_text),
            confirmText = stringResource(R.string.entry_leave_confirm),
            dismissText = stringResource(R.string.entry_leave_stay),
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )

        is PresetEditDialog.FieldsDropped -> ConfirmDialog(
            title = stringResource(R.string.preset_fields_dropped_title),
            text = stringResource(R.string.preset_fields_dropped_text),
            confirmText = stringResource(R.string.action_save),
            dismissText = stringResource(R.string.action_cancel),
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )
    }
}

/** Why the name of a field of a preset cannot be used - in the dialog and under the field. */
@Composable
internal fun fieldNameProblemText(problem: FieldNameProblem): String = when (problem) {
    FieldNameProblem.EMPTY -> stringResource(R.string.error_name_empty)
    FieldNameProblem.TAKEN -> stringResource(R.string.entry_field_name_taken)
}

/** Why the name of a preset cannot be used - under the field. */
@Composable
internal fun presetNameProblemText(problem: PresetNameProblem): String = when (problem) {
    PresetNameProblem.EMPTY -> stringResource(R.string.error_name_empty)
    PresetNameProblem.TOO_LONG -> pluralStringResource(
        R.plurals.preset_name_too_long,
        PresetLimits.MAX_NAME_LENGTH,
        PresetLimits.MAX_NAME_LENGTH,
    )
    PresetNameProblem.TAKEN -> stringResource(R.string.preset_name_taken)
}
