package ru.kino.dev.vault.pres.common

import androidx.compose.runtime.Composable
import ru.kino.dev.core.pres.*
import ru.kino.dev.ui.components.ConfirmDialog
import ru.kino.dev.ui.res.stringResource

/**
 * "Delete the entry?" - into the recycle bin, or for good when the database does not use one; from its card in the
 * list, or from its reading or its form (pass-docs, Step 50, item 2; Step 70).
 *
 * @param title of the entry as the file holds it; empty - "untitled"
 * @param toBin the delete goes to the recycle bin; false - for good, and the text says so
 * @param deleting the delete is under way: the dialog shows progress and takes nothing
 */
@Composable
internal fun EntryDeleteDialog(
    title: String,
    toBin: Boolean,
    deleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val name = title.ifEmpty { stringResource(Res.string.entry_untitled) }
    ConfirmDialog(
        title = stringResource(Res.string.entry_delete_title),
        text = stringResource(if (toBin) Res.string.entry_delete_text else Res.string.entry_delete_forever_text, name),
        confirmText = stringResource(Res.string.action_delete),
        dismissText = stringResource(Res.string.action_cancel),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        destructive = true,
        loading = deleting,
    )
}
