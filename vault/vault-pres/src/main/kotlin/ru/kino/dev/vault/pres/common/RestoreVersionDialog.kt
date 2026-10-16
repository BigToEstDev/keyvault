package ru.kino.dev.vault.pres.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ru.kino.dev.core.pres.R
import ru.kino.dev.ui.components.ConfirmDialog

/**
 * "Restore the version?" - the version saved at [modified] becomes the entry, the current state stays in the
 * history; from the menu of its card in the history, or from the version itself (pass-docs, Step 51).
 *
 * @param restoring the restore is under way: the dialog shows progress and takes nothing
 */
@Composable
internal fun RestoreVersionDialog(modified: Long, restoring: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    ConfirmDialog(
        title = stringResource(R.string.history_restore_title),
        text = stringResource(R.string.history_restore_text, entryTimeText(modified)),
        confirmText = stringResource(R.string.history_restore_confirm),
        dismissText = stringResource(R.string.action_cancel),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        loading = restoring,
    )
}
