package ru.kino.dev.vault.pres.grouplist.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ru.kino.dev.ui.components.CountBadgeIcon
import ru.kino.dev.vault.pres.grouplist.GroupItem

/** A group in the list: a folder with the number of entries in it and below it, and the name. */
@Composable
internal fun GroupCard(
    group: GroupItem,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListCard(
        icon = { CountBadgeIcon(count = group.entryCount) },
        title = group.name,
        onOpen = onOpen,
        onEdit = onEdit,
        onDelete = onDelete,
        modifier = modifier,
    )
}
