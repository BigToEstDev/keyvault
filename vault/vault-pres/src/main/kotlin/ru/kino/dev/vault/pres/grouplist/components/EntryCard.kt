package ru.kino.dev.vault.pres.grouplist.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ru.kino.dev.core.pres.R
import ru.kino.dev.vault.pres.grouplist.EntryItem

/**
 * An entry in the list: the icon of an entry and the title - "untitled" when it has none.
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
    ListCard(
        // The color of the folder of a group card, so the two kinds of cards match
        icon = { Icon(Icons.Filled.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = entry.title.ifEmpty { stringResource(R.string.entry_untitled) },
        subtitle = location,
        onOpen = onOpen,
        onEdit = onEdit,
        onDelete = onDelete,
        modifier = modifier,
    )
}
