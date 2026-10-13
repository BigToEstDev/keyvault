package ru.kino.dev.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp

/**
 * A dialog that asks for one line of text - a name - and opens with the keyboard up.
 *
 * Without state of its own: the text, the error and the wait all come from the caller.
 * While [loading] the confirming button shows progress, nothing takes clicks and the dialog does not close -
 * what it confirms is under way, and a second tap must not start it again.
 *
 * @param error marks the field as wrong and puts a "!" in it; the text is a bubble under the field - see
 * [TextField]
 */
@Composable
fun TextInputDialog(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    confirmText: String,
    dismissText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    error: String? = null,
    loading: Boolean = false,
) {
    val focus = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = { if (!loading) onDismiss() },
        title = { Text(title) },
        text = {
            // Here, inside the dialog: it is a window of its own, composed after the caller, and the field
            // has to exist before it can take focus
            val keyboard = LocalSoftwareKeyboardController.current
            LaunchedEffect(Unit) {
                focus.requestFocus()
                keyboard?.show()
            }
            TextField(
                value = value,
                onValueChange = onValueChange,
                label = label,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus),
                enabled = !loading,
                error = error,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { if (!loading) onConfirm() }),
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !loading) {
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
