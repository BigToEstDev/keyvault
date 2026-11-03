package ru.kino.dev.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.kino.dev.core.pres.Res
import ru.kino.dev.core.pres.action_close
import ru.kino.dev.ui.res.stringResource

/** A tab of [ClosableTabs]: [title] on it, [key] - what it stands for. */
data class ClosableTab(val key: String, val title: String)

/**
 * Tabs in a row that scrolls, each with a cross that closes it - the open documents of an editor (pass-docs, Step 98:
 * the entries open in the container of `vault`). The colors are the theme's; the row is transparent - it lies on what
 * is under it.
 *
 * @param selected the key of the tab shown; null - none
 */
@Composable
fun ClosableTabs(
    tabs: List<ClosableTab>,
    selected: String?,
    onSelect: (String) -> Unit,
    onClose: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val index = tabs.indexOfFirst { it.key == selected }.coerceAtLeast(0)
    PrimaryScrollableTabRow(
        selectedTabIndex = index,
        modifier = modifier,
        containerColor = Color.Transparent,
        edgePadding = 0.dp,
    ) {
        tabs.forEach { tab ->
            Tab(
                selected = tab.key == selected,
                onClick = { onSelect(tab.key) },
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp)) {
                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = TITLE_MAX_WIDTH),
                    )
                    IconButton(onClick = { onClose(tab.key) }) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = stringResource(Res.string.action_close),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

private val TITLE_MAX_WIDTH = 200.dp
