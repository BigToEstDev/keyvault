package ru.kino.dev.vault.pres.trash.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.kino.dev.core.pres.R
import ru.kino.dev.vault.pres.common.entryDateText
import ru.kino.dev.vault.trash.TrashEntry

/**
 * An entry of the recycle bin: the icon of an entry, the title - "untitled" when it has none - and under it the day
 * it was deleted (pass-docs, Step 70); a tap reads it (Step 71). While choosing, a check box in place of the icon,
 * and a picked card is tinted.
 *
 * @param choosing a choice goes on: a tap picks or lets go
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TrashCard(
    item: TrashEntry,
    choosing: Boolean,
    picked: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = if (picked) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        } else {
            CardDefaults.cardColors()
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onTap, onLongClick = onLongPress)
                .padding(horizontal = 16.dp, vertical = if (choosing) 8.dp else 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (choosing) {
                // Of the card: a tap on the box is a tap on the card
                Checkbox(checked = picked, onCheckedChange = { onTap() }, modifier = Modifier.size(48.dp))
            } else {
                Icon(Icons.Filled.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Column(modifier = Modifier.padding(start = if (choosing) 12.dp else 24.dp)) {
                Text(
                    text = item.entry.title.ifEmpty { stringResource(R.string.entry_untitled) },
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.trash_deleted_at, entryDateText(item.deletedAt)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}
