package ru.kino.dev.vault.pres.entryhistory.components

import androidx.compose.runtime.Composable
import ru.kino.dev.core.pres.*
import ru.kino.dev.ui.components.ConfirmDialog
import ru.kino.dev.ui.res.stringResource
import ru.kino.dev.vault.pres.common.RestoreVersionDialog
import ru.kino.dev.vault.pres.common.entryTimeText
import ru.kino.dev.vault.pres.entryhistory.EntryHistoryDialog

/** The dialog over the history, if any (pass-docs, Step 51, item 4). */
@Composable
internal fun EntryHistoryDialogs(dialog: EntryHistoryDialog?, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    when (dialog) {
        null -> Unit

        is EntryHistoryDialog.Restore -> RestoreVersionDialog(
            modified = dialog.modified,
            restoring = dialog.saving,
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )

        is EntryHistoryDialog.Delete -> ConfirmDialog(
            title = stringResource(Res.string.history_delete_title),
            text = stringResource(Res.string.history_delete_text, entryTimeText(dialog.modified)),
            confirmText = stringResource(Res.string.action_delete),
            dismissText = stringResource(Res.string.action_cancel),
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            destructive = true,
            loading = dialog.saving,
        )

        is EntryHistoryDialog.DeleteAll -> ConfirmDialog(
            title = stringResource(Res.string.history_delete_all_title),
            text = stringResource(Res.string.history_delete_all_text),
            confirmText = stringResource(Res.string.action_delete),
            dismissText = stringResource(Res.string.action_cancel),
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            destructive = true,
            loading = dialog.saving,
        )
    }
}
