package ru.kino.dev.vault.pres.common

import androidx.compose.runtime.Composable
import ru.kino.dev.core.pres.*
import ru.kino.dev.ui.components.ConfirmDialog
import ru.kino.dev.ui.res.stringResource

/**
 * "Restore the version?" - the version saved at [modified] becomes the entry, the current state stays in the
 * history; from the menu of its card in the history, or from the version itself (pass-docs, Step 51).
 *
 * @param restoring the restore is under way: the dialog shows progress and takes nothing
 */
@Composable
internal fun RestoreVersionDialog(modified: Long, restoring: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    ConfirmDialog(
        title = stringResource(Res.string.history_restore_title),
        text = stringResource(Res.string.history_restore_text, entryTimeText(modified)),
        confirmText = stringResource(Res.string.history_restore_confirm),
        dismissText = stringResource(Res.string.action_cancel),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        loading = restoring,
    )
}
