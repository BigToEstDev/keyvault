package ru.kino.dev.settings.pres.presetedit.components

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ru.kino.dev.ui.components.FieldMenuButton
import ru.kino.dev.ui.components.TextField

/**
 * A field of a preset on its form (pass-docs, Step 49, answer 10): it has a name and no value yet, so it is
 * drawn as the entry will show it - an empty field with its name on the border - and takes no input. One the
 * person named has the pen of [FieldMenuButton] by it; a fixed one - the user name, the password, the notes -
 * has none and takes the card's full width, as the name of the preset does (pass-docs, Step 40, fix 18.2).
 *
 * @param error why its name cannot be saved, if it cannot
 * @param onRename with [onDelete]: null for a fixed field
 */
@Composable
internal fun PresetFieldRow(
    name: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    required: Boolean = false,
    error: String? = null,
    onRename: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        TextField(
            value = "",
            onValueChange = {},
            label = name,
            modifier = Modifier.weight(1f),
            required = required,
            error = error,
            enabled = enabled,
            readOnly = true,
        )
        if (onRename != null && onDelete != null) {
            FieldMenuButton(enabled = enabled, onRename = onRename, onDelete = onDelete)
        }
    }
}
