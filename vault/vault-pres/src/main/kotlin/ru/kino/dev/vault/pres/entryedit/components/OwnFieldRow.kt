package ru.kino.dev.vault.pres.entryedit.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.kino.dev.ui.components.FieldMenuButton
import ru.kino.dev.ui.components.TextField
import ru.kino.dev.ui.components.VisibilityToggle
import ru.kino.dev.vault.entry.EntryField

/**
 * An own field on the form: the field, and outside it the pen of [FieldMenuButton] - rename, delete
 * (pass-docs, vault-our.md "Свое поле").
 *
 * A private one - protected in a database of another program, or by the settings of the database - is hidden
 * behind dots with an eye, as the password is; one added here is plain text.
 *
 * It wraps and grows with the text as notes do, from one line; behind dots - in one line (pass-docs, Step 75,
 * item 2). The pen stays by its first line, not in the middle of a tall field.
 *
 * @param private hidden behind dots, with the eye
 * @param error why its name cannot be saved, if it cannot
 */
@Composable
internal fun OwnFieldRow(
    field: EntryField,
    private: Boolean,
    shown: Boolean,
    error: String?,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    onToggleShown: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.Top) {
        TextField(
            value = field.value,
            onValueChange = onValueChange,
            label = field.key,
            modifier = Modifier.weight(1f),
            error = error,
            enabled = enabled,
            hidden = private && !shown,
            singleLine = private && !shown,
            // Shown in clear, colored as a password is (pass-docs, Step 58)
            secret = private,
            actions = if (private) {
                { VisibilityToggle(visible = shown, onToggle = onToggleShown, enabled = enabled) }
            } else {
                null
            },
        )
        Box(modifier = Modifier.padding(top = PEN_TOP)) {
            FieldMenuButton(enabled = enabled, onEdit = onEdit, onDelete = onDelete)
        }
    }
}

/**
 * The pen down from the top of the row - in the middle of a field of one line: the field 64 dp tall (the 8 dp
 * over its border the label takes, and 56), the button 48.
 */
private val PEN_TOP = 8.dp
