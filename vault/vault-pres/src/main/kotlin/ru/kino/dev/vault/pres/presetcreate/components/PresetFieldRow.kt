package ru.kino.dev.vault.pres.presetcreate.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.kino.dev.ui.components.TextField
import ru.kino.dev.vault.pres.common.FieldMenuButton

/**
 * A field of a preset on its form (pass-docs, Step 49, answer 10): it has a name and no value yet, so it is
 * drawn as the entry will show it - an empty field with its name on the border - and takes no input. One the
 * person named has the pen of [FieldMenuButton] by it; a fixed one - the user name, the password, the notes -
 * has none, and keeps its room so the fields stand in one column.
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
        } else {
            Spacer(Modifier.size(MENU_BUTTON_SIZE))
        }
    }
}

/** The room of the pen: an icon button of Material 3. */
private val MENU_BUTTON_SIZE = 48.dp
