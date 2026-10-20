package ru.kino.dev.vault.pres.entryedit.components

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ru.kino.dev.ui.components.TextField
import ru.kino.dev.ui.components.VisibilityToggle
import ru.kino.dev.vault.entry.EntryField
import ru.kino.dev.vault.pres.common.FieldMenuButton

/**
 * An own field on the form: the field, and outside it the pen of [FieldMenuButton] - rename, delete
 * (pass-docs, vault-our.md "Свое поле").
 *
 * A protected one - from a database of another program - is hidden behind dots with an eye, as the password
 * is; one added here is plain text.
 *
 * @param error why its name cannot be saved, if it cannot
 */
@Composable
internal fun OwnFieldRow(
    field: EntryField,
    shown: Boolean,
    error: String?,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    onToggleShown: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        TextField(
            value = field.value,
            onValueChange = onValueChange,
            label = field.key,
            modifier = Modifier.weight(1f),
            error = error,
            enabled = enabled,
            hidden = field.protected && !shown,
            // Shown in clear, colored as a password is (pass-docs, Step 58)
            secret = field.protected,
            actions = if (field.protected) {
                { VisibilityToggle(visible = shown, onToggle = onToggleShown, enabled = enabled) }
            } else {
                null
            },
        )
        FieldMenuButton(enabled = enabled, onRename = onRename, onDelete = onDelete)
    }
}
