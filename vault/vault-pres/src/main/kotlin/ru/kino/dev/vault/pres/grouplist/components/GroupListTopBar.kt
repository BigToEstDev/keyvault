package ru.kino.dev.vault.pres.grouplist.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.kino.dev.core.pres.R
import ru.kino.dev.ui.components.Breadcrumbs
import ru.kino.dev.ui.components.CountBadgeIcon
import ru.kino.dev.ui.components.SearchBar
import ru.kino.dev.vault.pres.grouplist.Crumb

/**
 * The top of the list: at the root - the database, its entry count on a folder and its name; inside a group
 * - "back" and the path from the database down to the group. Under either, the search field and the
 * filter button to the right of it.
 *
 * The database is named by its root group, which is what the tree has. A root without a name - another
 * program may write one - shows the usual "Database".
 */
@Composable
internal fun GroupListTopBar(
    path: List<Crumb>,
    entryCount: Int,
    query: String,
    onQueryChange: (String) -> Unit,
    onFilters: () -> Unit,
    onBack: () -> Unit,
    onCrumb: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val fallback = stringResource(R.string.entry_database_fallback)
    val names = path.mapIndexed { index, crumb -> if (index == 0) crumb.name.ifBlank { fallback } else crumb.name }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (path.size > 1) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.vault_back))
                }
                Breadcrumbs(items = names, onClick = onCrumb, modifier = Modifier.weight(1f))
            } else {
                CountBadgeIcon(count = entryCount, modifier = Modifier.padding(horizontal = 8.dp))
                Text(
                    text = names.firstOrNull().orEmpty(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 16.dp),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Nothing searches yet - the search is pass-docs, Step 41
            SearchBar(
                query = query,
                onQueryChange = onQueryChange,
                placeholder = stringResource(R.string.vault_search),
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onFilters) {
                Icon(Icons.Filled.Tune, contentDescription = stringResource(R.string.vault_filters))
            }
        }
    }
}
