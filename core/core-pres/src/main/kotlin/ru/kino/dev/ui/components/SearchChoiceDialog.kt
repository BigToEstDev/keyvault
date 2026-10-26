package ru.kino.dev.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import ru.kino.dev.core.pres.R

/** One choice of a [SearchChoiceDialog]: [lead], when there is one, goes before the [title] - the prefix of a country. */
data class SearchChoice<T>(val value: T, val title: String, val lead: String? = null)

/**
 * A choice out of a long list, with a search over it (pass-docs, Step 77, decision 5): the look of the choice of
 * presets - the title, a "close" button, a list of tiles, the chosen one filled with the accent container; a tap
 * picks one and closes. The search sits over the list and keeps the choices whose title or lead holds what is
 * typed, ignoring case; the list opens at the chosen one.
 */
@Composable
fun <T> SearchChoiceDialog(
    title: String,
    choices: List<SearchChoice<T>>,
    selected: T?,
    onChoose: (T) -> Unit,
    onClose: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val shown = remember(choices, query) {
        val term = query.trim()
        if (term.isEmpty()) {
            choices
        } else {
            choices.filter { it.title.contains(term, ignoreCase = true) || it.lead?.contains(term, ignoreCase = true) == true }
        }
    }
    val list = rememberLazyListState(initialFirstVisibleItemIndex = choices.indexOfFirst { it.value == selected }.coerceAtLeast(0))

    Dialog(onDismissRequest = onClose) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // The end is narrower - the "close" button sits there; the list keeps the start's width
                    .padding(start = 24.dp, end = 8.dp, top = 16.dp, bottom = 24.dp),
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_close))
                    }
                }
                SearchBar(
                    query = query,
                    onQueryChange = { query = it },
                    placeholder = stringResource(R.string.vault_search),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, end = 16.dp),
                )
                LazyColumn(
                    state = list,
                    modifier = Modifier
                        .padding(top = 12.dp, end = 16.dp)
                        .heightIn(max = LIST_MAX_HEIGHT),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(shown) { choice ->
                        ChoiceTile(choice = choice, selected = choice.value == selected, onClick = { onChoose(choice.value) })
                    }
                }
                if (shown.isEmpty()) {
                    Text(
                        text = stringResource(R.string.vault_search_nothing_found, query.trim()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp, end = 16.dp),
                    )
                }
            }
        }
    }
}

/** A choice of the list: a plain tile, the chosen one told by its fill and, to TalkBack, by the selected state. */
@Composable
private fun <T> ChoiceTile(choice: SearchChoice<T>, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        selected = selected,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) colors.primaryContainer else colors.onSurface.copy(alpha = TILE_FILL_ALPHA),
        contentColor = if (selected) colors.onPrimaryContainer else colors.onSurface,
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            choice.lead?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .widthIn(min = LEAD_WIDTH)
                        .padding(end = 12.dp),
                )
            }
            Text(text = choice.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** The list scrolls past this, the search staying on top. */
private val LIST_MAX_HEIGHT = 360.dp

/** The leads line up in a column: wide enough for the longest prefix, `+1 868`. */
private val LEAD_WIDTH = 56.dp

/** A tile not chosen - barely set off the dialog, in either theme, as in the choice of presets. */
private const val TILE_FILL_ALPHA = 0.05f
