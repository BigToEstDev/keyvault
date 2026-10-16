package ru.kino.dev.vault.pres.entryhistory.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ru.kino.dev.core.pres.R
import ru.kino.dev.ui.components.ConfirmDialog
import ru.kino.dev.vault.pres.common.entryTimeText
import ru.kino.dev.vault.pres.entryhistory.EntryHistoryDialog

/** The dialog over the history, if any (pass-docs, Step 51, item 4). */
@Composable
internal fun EntryHistoryDialogs(dialog: EntryHistoryDialog?, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    when (dialog) {
        null -> Unit

        is EntryHistoryDialog.Restore -> ConfirmDialog(
            title = stringResource(R.string.history_restore_title),
            text = stringResource(R.string.history_restore_text, entryTimeText(dialog.modified)),
            confirmText = stringResource(R.string.history_restore_confirm),
            dismissText = stringResource(R.string.action_cancel),
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            loading = dialog.saving,
        )

        is EntryHistoryDialog.Delete -> ConfirmDialog(
            title = stringResource(R.string.history_delete_title),
            text = stringResource(R.string.history_delete_text, entryTimeText(dialog.modified)),
            confirmText = stringResource(R.string.group_delete_confirm),
            dismissText = stringResource(R.string.action_cancel),
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            destructive = true,
            loading = dialog.saving,
        )
    }
}
