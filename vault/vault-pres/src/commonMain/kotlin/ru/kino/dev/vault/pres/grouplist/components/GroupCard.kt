package ru.kino.dev.vault.pres.grouplist.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ru.kino.dev.ui.components.CountBadgeIcon
import ru.kino.dev.ui.components.deleteMenuItem
import ru.kino.dev.ui.components.editMenuItem
import ru.kino.dev.vault.pres.common.MenuCard
import ru.kino.dev.vault.pres.grouplist.GroupItem

/**
 * A group in the list: a folder with the number of entries in it and below it, and the name.
 *
 * @param location where it lies, when a search found it
 */
@Composable
internal fun GroupCard(
    group: GroupItem,
    location: String?,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MenuCard(
        icon = { CountBadgeIcon(count = group.entryCount.shown()) },
        title = group.name,
        subtitle = location,
        items = listOf(editMenuItem(onEdit), deleteMenuItem(onDelete)),
        onOpen = onOpen,
        modifier = modifier,
    )
}
