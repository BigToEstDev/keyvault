package ru.kino.dev.vault.pres.trash.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ru.kino.dev.core.pres.*
import ru.kino.dev.ui.components.ConfirmDialog
import ru.kino.dev.ui.components.Dropdown
import ru.kino.dev.ui.res.pluralStringResource
import ru.kino.dev.ui.res.stringResource
import ru.kino.dev.vault.group.GroupId
import ru.kino.dev.vault.pres.common.option
import ru.kino.dev.vault.pres.trash.TrashDialog

/**
 * The dialog over the bin: where to restore the picked entries - one group for all of them - or "delete for good?",
 * of the picked ones or of the whole bin (pass-docs, Step 70).
 */
@Composable
internal fun TrashDialogs(
    dialog: TrashDialog?,
    onGroup: (GroupId) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    when (dialog) {
        null -> Unit

        is TrashDialog.Restore -> ConfirmDialog(
            title = stringResource(Res.string.trash_restore_title),
            text = stringResource(Res.string.trash_restore_text),
            confirmText = stringResource(Res.string.action_restore),
            dismissText = stringResource(Res.string.action_cancel),
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            loading = dialog.saving,
            confirmEnabled = dialog.group != null,
        ) {
            Dropdown(
                selected = dialog.group,
                options = dialog.groups.map { it.option() },
                onSelect = onGroup,
                label = stringResource(Res.string.entry_field_group),
                modifier = Modifier.fillMaxWidth(),
                enabled = !dialog.saving,
            )
        }

        is TrashDialog.Remove -> ConfirmDialog(
            title = stringResource(Res.string.trash_remove_title),
            text = pluralStringResource(Res.plurals.trash_remove_text, dialog.count, dialog.count),
            confirmText = stringResource(Res.string.action_delete),
            dismissText = stringResource(Res.string.action_cancel),
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            destructive = true,
            loading = dialog.saving,
        )

        is TrashDialog.Empty -> ConfirmDialog(
            title = stringResource(Res.string.trash_empty_title),
            text = stringResource(Res.string.trash_empty_text),
            confirmText = stringResource(Res.string.trash_empty_confirm),
            dismissText = stringResource(Res.string.action_cancel),
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            destructive = true,
            loading = dialog.saving,
        )
    }
}
