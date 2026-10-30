package ru.kino.dev.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import ru.kino.dev.core.pres.*
import ru.kino.dev.ui.res.stringResource

/**
 * Puts a value of an entry into the clipboard of the system - marked sensitive where the system knows the mark.
 *
 * The clipboard is not cleared afterwards, on the desktop either - more trouble than use (pass-docs, Step 46,
 * Step 89).
 */
interface SensitiveClipboard {

    /** Copies [value]; [label] names it for the system - the name of the field, never the value. */
    fun copy(label: String, value: String)
}

@Composable
expect fun rememberSensitiveClipboard(): SensitiveClipboard

/** The button that copies the value of a field. */
@Composable
fun CopyButton(onCopy: () -> Unit, enabled: Boolean = true) {
    IconButton(onClick = onCopy, enabled = enabled) {
        Icon(imageVector = Icons.Filled.ContentCopy, contentDescription = stringResource(Res.string.action_copy))
    }
}
