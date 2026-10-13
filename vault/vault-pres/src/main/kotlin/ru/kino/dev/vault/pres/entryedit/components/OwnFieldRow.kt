package ru.kino.dev.vault.pres.entryedit.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ru.kino.dev.core.pres.R
import ru.kino.dev.ui.components.TextField
import ru.kino.dev.ui.components.VisibilityToggle
import ru.kino.dev.vault.entry.EntryField

/**
 * An own field on the form: the field, and outside it - as the filter button sits by the search - a pen that
 * opens a menu: rename, delete (pass-docs, vault-our.md "Свое поле").
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
    var menu by remember { mutableStateOf(false) }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        TextField(
            value = field.value,
            onValueChange = onValueChange,
            label = field.key,
            modifier = Modifier.weight(1f),
            error = error,
            enabled = enabled,
            hidden = field.protected && !shown,
            actions = if (field.protected) {
                { VisibilityToggle(visible = shown, onToggle = onToggleShown, enabled = enabled) }
            } else {
                null
            },
        )
        Box {
            IconButton(onClick = { menu = true }, enabled = enabled) {
                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.entry_field_menu))
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.entry_field_rename)) },
                    leadingIcon = { Icon(Icons.Filled.DriveFileRenameOutline, contentDescription = null) },
                    onClick = {
                        menu = false
                        onRename()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.entry_field_delete)) },
                    leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                    onClick = {
                        menu = false
                        onDelete()
                    },
                )
            }
        }
    }
}
