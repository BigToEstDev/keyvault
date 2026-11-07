package ru.kino.dev.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import ru.kino.dev.core.pres.Res
import ru.kino.dev.core.pres.action_ok

/**
 * Something said to the person - what came out, what failed - closed by "OK" (pass-docs, `CLAUDE.md`: no snackbars,
 * a dialog). Closing past it closes it too: there is nothing to answer.
 */
@Composable
fun MessageDialog(
    title: String,
    text: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_ok)) }
        },
    )
}
