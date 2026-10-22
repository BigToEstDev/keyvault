package ru.kino.dev.vault.pres.trash.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ru.kino.dev.core.pres.R
import ru.kino.dev.vault.pres.common.MenuCard
import ru.kino.dev.vault.pres.common.entryDateText
import ru.kino.dev.vault.trash.TrashEntry

/**
 * An entry of the recycle bin: the icon of an entry, the title - "untitled" when it has none - and under it the day
 * it was deleted (pass-docs, Step 70). A tap does nothing: an entry in the bin is not read.
 */
@Composable
internal fun TrashCard(item: TrashEntry, modifier: Modifier = Modifier) {
    MenuCard(
        icon = { Icon(Icons.Filled.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = item.entry.title.ifEmpty { stringResource(R.string.entry_untitled) },
        detail = stringResource(R.string.trash_deleted_at, entryDateText(item.deletedAt)),
        items = emptyList(),
        modifier = modifier,
    )
}
