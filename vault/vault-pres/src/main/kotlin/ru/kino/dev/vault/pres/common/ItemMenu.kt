package ru.kino.dev.vault.pres.common

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Restore
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import ru.kino.dev.core.pres.R

/** One choice of [ItemMenu]: what it says, its icon, what it does. */
internal data class MenuItem(val text: String, val icon: ImageVector, val onClick: () -> Unit)

/** "Edit" - a group or an entry. */
@Composable
internal fun editMenuItem(onClick: () -> Unit) =
    MenuItem(stringResource(R.string.vault_card_edit), Icons.Filled.Edit, onClick)

/** "Delete" - a group or an entry. */
@Composable
internal fun deleteMenuItem(onClick: () -> Unit) =
    MenuItem(stringResource(R.string.vault_card_delete), Icons.Filled.Delete, onClick)

/** "History" - the old versions of an entry (pass-docs, Step 51, item 3). */
@Composable
internal fun historyMenuItem(onClick: () -> Unit) =
    MenuItem(stringResource(R.string.entry_history), Icons.Filled.History, onClick)

/** "Restore this version" - of a history (pass-docs, Step 51, item 4). */
@Composable
internal fun restoreMenuItem(onClick: () -> Unit) =
    MenuItem(stringResource(R.string.history_restore), Icons.Filled.Restore, onClick)

/** "Delete history" - every old version of an entry (pass-docs, Step 51, item 5). */
@Composable
internal fun deleteHistoryMenuItem(onClick: () -> Unit) =
    MenuItem(stringResource(R.string.history_delete_all), Icons.Filled.Delete, onClick)

/** "Delete version" - of a history. */
@Composable
internal fun deleteVersionMenuItem(onClick: () -> Unit) =
    MenuItem(stringResource(R.string.history_delete_version), Icons.Filled.Delete, onClick)

/**
 * The menu of a group or an entry - [items] in their order - under its anchor: a card held long, or the
 * button of a top bar. A choice closes it first.
 */
@Composable
internal fun ItemMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    items: List<MenuItem>,
    offset: DpOffset = DpOffset.Zero,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss, offset = offset) {
        items.forEach { item ->
            DropdownMenuItem(
                text = { Text(item.text) },
                leadingIcon = { Icon(item.icon, contentDescription = null) },
                onClick = {
                    onDismiss()
                    item.onClick()
                },
            )
        }
    }
}

/**
 * The button of a top bar - three dots - with [ItemMenu] under it, for the group or the entry the screen
 * shows (pass-docs, Step 50). Whether the menu is open is the button's own.
 */
@Composable
internal fun MoreMenuButton(contentDescription: String, items: List<MenuItem>, enabled: Boolean = true) {
    var menu by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { menu = true }, enabled = enabled) {
            Icon(Icons.Filled.MoreVert, contentDescription = contentDescription)
        }
        ItemMenu(expanded = menu, onDismiss = { menu = false }, items = items)
    }
}
