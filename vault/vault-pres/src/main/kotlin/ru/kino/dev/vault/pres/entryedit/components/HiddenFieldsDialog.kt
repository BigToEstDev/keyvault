package ru.kino.dev.vault.pres.entryedit.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import ru.kino.dev.core.pres.R
import ru.kino.dev.vault.pres.common.fieldLabel

/**
 * "Save" with fields the preset chosen lacks - filled ones of a preset chosen before (pass-docs, Step 49,
 * answer 9). Three ways: save as is - they are lost; add them outside the preset - as own fields; cancel.
 *
 * A dialog of its own, not `ConfirmDialog` with a third button (owner, Step 49). The three labels are long,
 * so the buttons stand one under another, at the end.
 */
@Composable
internal fun HiddenFieldsDialog(
    keys: List<String>,
    onSaveAsIs: () -> Unit,
    onKeep: () -> Unit,
    onCancel: () -> Unit,
) {
    val names = keys.map { fieldLabel(it) }.joinToString()
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.entry_hidden_title)) },
        text = { Text(stringResource(R.string.entry_hidden_text, names)) },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                TextButton(onClick = onSaveAsIs) { Text(stringResource(R.string.entry_hidden_save)) }
                TextButton(onClick = onKeep) { Text(stringResource(R.string.entry_hidden_keep)) }
                TextButton(onClick = onCancel) { Text(stringResource(R.string.action_cancel)) }
            }
        },
    )
}
