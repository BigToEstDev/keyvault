package ru.kino.dev.vault.pres.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ru.kino.dev.core.pres.R
import ru.kino.dev.ui.components.ConfirmDialog

/**
 * "Delete the entry?" - into the recycle bin; from its card in the list, or from its reading or its form
 * (pass-docs, Step 50, item 2).
 *
 * @param title of the entry as the file holds it; empty - "untitled"
 * @param deleting the delete is under way: the dialog shows progress and takes nothing
 */
@Composable
internal fun EntryDeleteDialog(title: String, deleting: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    ConfirmDialog(
        title = stringResource(R.string.entry_delete_title),
        text = stringResource(R.string.entry_delete_text, title.ifEmpty { stringResource(R.string.entry_untitled) }),
        confirmText = stringResource(R.string.group_delete_confirm),
        dismissText = stringResource(R.string.action_cancel),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        destructive = true,
        loading = deleting,
    )
}
