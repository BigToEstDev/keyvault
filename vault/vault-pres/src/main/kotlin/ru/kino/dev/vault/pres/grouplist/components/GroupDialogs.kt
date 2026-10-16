package ru.kino.dev.vault.pres.grouplist.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import ru.kino.dev.core.pres.R
import ru.kino.dev.ui.components.ConfirmDialog
import ru.kino.dev.ui.components.TextInputDialog
import ru.kino.dev.vault.group.GroupNameValidator
import ru.kino.dev.vault.pres.common.EntryDeleteDialog
import ru.kino.dev.vault.pres.grouplist.GroupDialog
import ru.kino.dev.vault.pres.grouplist.NameProblem

/** The dialog over the list: the name of a new or renamed group, or "delete?" - a group or an entry. */
@Composable
internal fun GroupDialogs(
    dialog: GroupDialog?,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    when (dialog) {
        null -> Unit

        is GroupDialog.Create -> NameDialog(
            title = stringResource(R.string.group_dialog_create_title),
            name = dialog.name,
            problem = dialog.problem,
            saving = dialog.saving,
            onNameChange = onNameChange,
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )

        is GroupDialog.Rename -> NameDialog(
            title = stringResource(R.string.group_dialog_rename_title),
            name = dialog.name,
            problem = dialog.problem,
            saving = dialog.saving,
            onNameChange = onNameChange,
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )

        is GroupDialog.DeleteEntry -> EntryDeleteDialog(
            title = dialog.title,
            deleting = dialog.saving,
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )

        is GroupDialog.Delete -> ConfirmDialog(
            title = stringResource(R.string.group_delete_title),
            text = stringResource(R.string.group_delete_text, dialog.name),
            confirmText = stringResource(R.string.group_delete_confirm),
            dismissText = stringResource(R.string.action_cancel),
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            destructive = true,
            loading = dialog.saving,
        )
    }
}

@Composable
private fun NameDialog(
    title: String,
    name: String,
    problem: NameProblem?,
    saving: Boolean,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    TextInputDialog(
        title = title,
        value = name,
        onValueChange = onNameChange,
        label = stringResource(R.string.group_dialog_name),
        confirmText = stringResource(R.string.group_dialog_save),
        dismissText = stringResource(R.string.action_cancel),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        error = problem?.let { text(it) },
        loading = saving,
    )
}

@Composable
private fun text(problem: NameProblem): String = when (problem) {
    NameProblem.Empty -> stringResource(R.string.group_name_empty)
    NameProblem.TooLong -> pluralStringResource(
        R.plurals.group_name_too_long,
        GroupNameValidator.MAX_LENGTH,
        GroupNameValidator.MAX_LENGTH,
    )
    NameProblem.Taken -> stringResource(R.string.group_name_taken)
}
