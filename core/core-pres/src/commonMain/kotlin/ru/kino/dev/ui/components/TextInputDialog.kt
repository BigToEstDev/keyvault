package ru.kino.dev.ui.components

import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
 * @param secret a password rather than a name: a [PasswordField] with its eye, no capitals, no corrections
 * (pass-docs, Step 59, item 7)
 * @param below what goes under the field - a setting of what is named (pass-docs, Step 75)
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
    secret: Boolean = false,
    below: (@Composable () -> Unit)? = null,
) {
    val focus = remember { FocusRequester() }
    var visible by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!loading) onDismiss() },
        title = { Text(title) },
        text = {
            Column {
                // Here, inside the dialog: it is a window of its own, composed after the caller, and the field
                // has to exist before it can take focus
                val keyboard = LocalSoftwareKeyboardController.current
                LaunchedEffect(Unit) {
                    focus.requestFocus()
                    keyboard?.show()
                }
                val modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus)
                if (secret) {
                    PasswordField(
                        value = value,
                        onValueChange = onValueChange,
                        label = label,
                        visible = visible,
                        onToggleVisible = { visible = !visible },
                        modifier = modifier,
                        error = error,
                        enabled = !loading,
                        onImeAction = { if (!loading) onConfirm() },
                    )
                } else {
                    TextField(
                        value = value,
                        onValueChange = onValueChange,
                        label = label,
                        modifier = modifier,
                        enabled = !loading,
                        error = error,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { if (!loading) onConfirm() }),
                    )
                }
                below?.invoke()
            }
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
