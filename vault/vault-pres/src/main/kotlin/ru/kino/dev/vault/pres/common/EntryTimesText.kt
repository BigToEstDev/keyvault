package ru.kino.dev.vault.pres.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.kino.dev.core.pres.*
import ru.kino.dev.ui.res.stringResource
import ru.kino.dev.vault.entry.EntryTimes

/**
 * Under the cards, in the middle: when the entry was last changed, and under it when it was made (pass-docs,
 * Step 51, item 2). A time that could not be read has no line. When an entry of the bin was deleted is under its
 * title, not here (pass-docs, Step 72, item 6).
 */
@Composable
internal fun EntryTimesText(times: EntryTimes) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        times.modified?.let { TimeLine(stringResource(Res.string.entry_modified_at, entryTimeText(it))) }
        times.created?.let { TimeLine(stringResource(Res.string.entry_created_at, entryTimeText(it))) }
    }
}

@Composable
private fun TimeLine(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
