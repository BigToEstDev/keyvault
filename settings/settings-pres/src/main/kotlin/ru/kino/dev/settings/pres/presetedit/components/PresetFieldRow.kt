package ru.kino.dev.settings.pres.presetedit.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.kino.dev.core.pres.*
import ru.kino.dev.ui.components.FieldMenuButton
import ru.kino.dev.ui.components.TextField
import ru.kino.dev.ui.res.stringResource

/**
 * A field of a preset on its form (pass-docs, Step 49, answer 10): it has a name and no value yet, so it is
 * drawn as the entry will show it - an empty field with its name on the border - and takes no input. One the
 * person named has the pen of [FieldMenuButton] by it; a fixed one - the user name, the password, the notes -
 * has none and takes the card's full width, as the name of the preset does (pass-docs, Step 40, fix 18.2).
 *
 * @param protected protected in a new entry - a lock at the end of the field, where the entry has its eye
 *   (pass-docs, Step 75)
 * @param error why its name cannot be saved, if it cannot
 * @param onEdit with [onDelete]: null for a fixed field
 */
@Composable
internal fun PresetFieldRow(
    name: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    required: Boolean = false,
    protected: Boolean = false,
    error: String? = null,
    onEdit: (() -> Unit)? = null,
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
            actions = if (protected) {
                {
                    // As wide as the eye of the entry: the button's room around the icon
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = stringResource(Res.string.field_protected),
                        modifier = Modifier.padding(12.dp),
                    )
                }
            } else {
                null
            },
        )
        if (onEdit != null && onDelete != null) {
            FieldMenuButton(enabled = enabled, onEdit = onEdit, onDelete = onDelete)
        }
    }
}
