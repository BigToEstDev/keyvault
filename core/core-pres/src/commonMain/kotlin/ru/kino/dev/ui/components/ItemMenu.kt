package ru.kino.dev.ui.components

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.DpOffset
import ru.kino.dev.core.pres.*
import ru.kino.dev.ui.res.stringResource

/** One choice of [ItemMenu]: what it says, its icon, what it does. */
data class MenuItem(val text: String, val icon: ImageVector, val onClick: () -> Unit)

/** "Edit" - a group, an entry, a preset, a field the person named. */
@Composable
fun editMenuItem(onClick: () -> Unit) = MenuItem(stringResource(Res.string.action_edit), Icons.Filled.Edit, onClick)

/** "Delete" - a group, an entry, a preset, a field. */
@Composable
fun deleteMenuItem(onClick: () -> Unit) = MenuItem(stringResource(Res.string.action_delete), Icons.Filled.Delete, onClick)

/**
 * The menu of an item - [items] in their order - under its anchor: a card held long, the button of a top bar,
 * the pen of a card. A choice closes it first (pass-docs, Step 62: shared by `vault` and `settings`).
 */
@Composable
fun ItemMenu(
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
 * A button with [ItemMenu] under it - [icon] and what it is for in [contentDescription]. Whether the menu is open
 * is the button's own.
 */
@Composable
fun MenuButton(
    icon: ImageVector,
    contentDescription: String,
    items: List<MenuItem>,
    enabled: Boolean = true,
) {
    var menu by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { menu = true }, enabled = enabled) {
            Icon(icon, contentDescription = contentDescription)
        }
        ItemMenu(expanded = menu, onDismiss = { menu = false }, items = items)
    }
}

/**
 * The button of a top bar - three dots - with [ItemMenu] under it, for what the screen shows (pass-docs,
 * Step 50).
 */
@Composable
fun MoreMenuButton(contentDescription: String, items: List<MenuItem>, enabled: Boolean = true) {
    MenuButton(Icons.Filled.MoreVert, contentDescription, items, enabled)
}
