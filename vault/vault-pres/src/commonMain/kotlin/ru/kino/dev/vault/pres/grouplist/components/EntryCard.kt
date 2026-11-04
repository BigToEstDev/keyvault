package ru.kino.dev.vault.pres.grouplist.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ru.kino.dev.core.pres.*
import ru.kino.dev.ui.components.deleteMenuItem
import ru.kino.dev.ui.components.editMenuItem
import ru.kino.dev.ui.res.stringResource
import ru.kino.dev.ui.theme.LocalAppDisplay
import ru.kino.dev.ui.components.MenuCard
import ru.kino.dev.vault.pres.grouplist.EntryItem

/**
 * An entry in the list: the icon of an entry and the title - "untitled" when it has none; under it the user
 * name, when the settings say so and the entry has one (pass-docs, Step 58).
 *
 * @param location where it lies, when a search found it
 */
@Composable
internal fun EntryCard(
    entry: EntryItem,
    location: String?,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MenuCard(
        // The color of the folder of a group card, so the two kinds of cards match
        icon = { Icon(Icons.Filled.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = entry.title.ifEmpty { stringResource(Res.string.entry_untitled) },
        detail = entry.userName.takeIf { LocalAppDisplay.current.userNameInList && it.isNotEmpty() },
        subtitle = location,
        items = listOf(editMenuItem(onEdit), deleteMenuItem(onDelete)),
        onOpen = onOpen,
        modifier = modifier,
    )
}
