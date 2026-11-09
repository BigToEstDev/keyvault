package ru.kino.dev.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

/**
 * A question with two answers. [destructive] paints the confirming one as an error - for answers that
 * cannot be undone, like writing over an existing file.
 *
 * While [loading] the confirming button shows progress, nothing takes clicks and the dialog does not close:
 * the answer is being carried out.
 *
 * @param confirmEnabled false - the answer waits for something chosen in [body]
 * @param body what is chosen before answering - a group to restore to, say - under the text
 * @param dismissOnOutside false - only the buttons close it: neither a tap past it nor "back" do
 */
@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmText: String,
    dismissText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
    loading: Boolean = false,
    confirmEnabled: Boolean = true,
    dismissOnOutside: Boolean = true,
    // Last: callers give it as a trailing lambda
    body: (@Composable () -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = { if (!loading) onDismiss() },
        properties = DialogProperties(dismissOnBackPress = dismissOnOutside, dismissOnClickOutside = dismissOnOutside),
        title = { Text(title) },
        text = {
            if (body == null) {
                Text(text)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(text)
                    body()
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !loading && confirmEnabled,
                colors = if (destructive) {
                    ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                } else {
                    ButtonDefaults.textButtonColors()
                },
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = LocalContentColor.current,
                    )
                } else {
                    Text(confirmText)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !loading) { Text(dismissText) }
        },
    )
}
