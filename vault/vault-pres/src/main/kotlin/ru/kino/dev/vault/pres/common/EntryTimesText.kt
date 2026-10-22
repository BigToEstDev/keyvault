package ru.kino.dev.vault.pres.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.kino.dev.core.pres.R
import ru.kino.dev.vault.entry.EntryTimes

/**
 * Under the cards, in the middle: when the entry was last changed, and under it when it was made (pass-docs,
 * Step 51, item 2). A time that could not be read has no line.
 *
 * @param deletedAt an entry of the recycle bin - when it went there: the last line, plainer to see than the times
 * above (pass-docs, Step 71)
 */
@Composable
internal fun EntryTimesText(times: EntryTimes, deletedAt: Long? = null) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        times.modified?.let { TimeLine(stringResource(R.string.entry_modified_at, entryTimeText(it))) }
        times.created?.let { TimeLine(stringResource(R.string.entry_created_at, entryTimeText(it))) }
        deletedAt?.let {
            Text(
                text = stringResource(R.string.trash_deleted_at, entryTimeText(it)),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun TimeLine(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
