package ru.kino.dev.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
 * Tabs in a row that scrolls - the open documents of an editor (pass-docs, Step 98: the entries open in the container
 * of `vault`). Each is a small card with its title and a cross that closes it (Step 99, item 4): the one shown in the
 * tone of the cards of a list, the others on nothing, an outline alone - no line under the tab shown.
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
    contentPadding: PaddingValues = PaddingValues(12.dp),
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(TAB_GAP),
    ) {
        tabs.forEach { tab ->
            TabCard(
                title = tab.title,
                selected = tab.key == selected,
                onSelect = { onSelect(tab.key) },
                onClose = { onClose(tab.key) },
            )
        }
    }
}

@Composable
private fun TabCard(title: String, selected: Boolean, onSelect: () -> Unit, onClose: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onSelect,
        modifier = Modifier.height(TAB_HEIGHT),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) colors.surfaceContainerHighest else Color.Transparent,
        contentColor = if (selected) colors.onSurface else colors.onSurfaceVariant,
        border = if (selected) null else BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 12.dp, end = 6.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = TITLE_MAX_WIDTH),
            )
            // Small - its highlight is no bigger than the cross itself; on a touch screen Material widens the touch
            // to its minimum unseen
            IconButton(onClick = onClose, modifier = Modifier.padding(start = 4.dp).size(CLOSE_SIZE)) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(Res.string.action_close),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

private val TAB_HEIGHT = 40.dp
private val TAB_GAP = 8.dp
private val CLOSE_SIZE = 28.dp
private val TITLE_MAX_WIDTH = 200.dp
