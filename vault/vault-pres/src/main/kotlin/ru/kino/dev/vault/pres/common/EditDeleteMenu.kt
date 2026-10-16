package ru.kino.dev.vault.pres.common

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import ru.kino.dev.core.pres.R

/**
 * The menu of a group or an entry - edit, delete - under its anchor: a card held long, or the button of a
 * top bar. Without [onEdit] it is "delete" alone - the screens of an entry are its reading and its form
 * already. A choice closes it first.
 */
@Composable
internal fun EditDeleteMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onEdit: (() -> Unit)? = null,
    offset: DpOffset = DpOffset.Zero,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss, offset = offset) {
        if (onEdit != null) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.vault_card_edit)) },
                leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                onClick = {
                    onDismiss()
                    onEdit()
                },
            )
        }
        DropdownMenuItem(
            text = { Text(stringResource(R.string.vault_card_delete)) },
            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
            onClick = {
                onDismiss()
                onDelete()
            },
        )
    }
}

/**
 * The button of a top bar - three dots - with [EditDeleteMenu] under it, for the group or the entry the
 * screen shows (pass-docs, Step 50). Whether the menu is open is the button's own.
 */
@Composable
internal fun MoreMenuButton(
    contentDescription: String,
    onDelete: () -> Unit,
    onEdit: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    var menu by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { menu = true }, enabled = enabled) {
            Icon(Icons.Filled.MoreVert, contentDescription = contentDescription)
        }
        EditDeleteMenu(expanded = menu, onDismiss = { menu = false }, onDelete = onDelete, onEdit = onEdit)
    }
}
