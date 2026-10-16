package ru.kino.dev.vault.pres.entryhistory.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ru.kino.dev.vault.pres.common.MenuCard
import ru.kino.dev.vault.pres.common.MenuItem

/**
 * A card of the history - "Modified: …" for an old version, "Created: …" last: the clock and the [text], the
 * way the cards of a group look (pass-docs, Step 51, item 3). A version has a menu on a long press - [items];
 * "Created" has none.
 */
@Composable
internal fun HistoryCard(text: String, modifier: Modifier = Modifier, items: List<MenuItem> = emptyList()) {
    MenuCard(
        icon = { Icon(Icons.Filled.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = text,
        items = items,
        modifier = modifier,
    )
}
