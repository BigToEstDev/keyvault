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
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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
import ru.kino.dev.ui.components.SkeletonText
import ru.kino.dev.ui.components.skeletonBrush
import ru.kino.dev.vault.pres.common.MoreMenuButton
import ru.kino.dev.vault.pres.common.deleteMenuItem
import ru.kino.dev.vault.pres.common.editMenuItem
import ru.kino.dev.vault.pres.grouplist.Crumb

/**
 * The top of the list: at the root - the database, its entry count on a folder and its name; inside a group
 * - "back", the path from the database down to the group, a skeleton of it while the tree is read, and the
 * menu of the group - edit, delete - once it is read (pass-docs, Step 50, item 1). Under
 * either, the search field and the filter button to the right of it - in the accent, with a dot over it,
 * while a filter other than the default is on.
 *
 * The database is named by its root group, which is what the tree has. A root without a name - another
 * program may write one - shows the usual "Database".
 */
@Composable
internal fun GroupListTopBar(
    isRoot: Boolean,
    path: List<Crumb>,
    entryCount: Int,
    query: String,
    filterOn: Boolean,
    onQueryChange: (String) -> Unit,
    onFilters: () -> Unit,
    onBack: () -> Unit,
    onCrumb: (index: Int) -> Unit,
    onEditGroup: () -> Unit,
    onDeleteGroup: () -> Unit,
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
            if (!isRoot) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.vault_back))
                }
                if (path.isEmpty()) {
                    // The tree is not read yet: a skeleton for the path, so it does not flash in
                    SkeletonText(
                        brush = skeletonBrush(),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                    )
                } else {
                    Breadcrumbs(items = names, onClick = onCrumb, modifier = Modifier.weight(1f))
                    MoreMenuButton(
                        contentDescription = stringResource(R.string.vault_group_menu),
                        items = listOf(editMenuItem(onEditGroup), deleteMenuItem(onDeleteGroup)),
                    )
                }
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
            SearchBar(
                query = query,
                onQueryChange = onQueryChange,
                placeholder = stringResource(R.string.vault_search),
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onFilters) {
                FilterIcon(on = filterOn)
            }
        }
    }
}

/** The filter button's icon: plain with the default filter; in the accent with a dot over it otherwise. */
@Composable
private fun FilterIcon(on: Boolean) {
    val description = stringResource(if (on) R.string.vault_filters_on else R.string.vault_filters)
    if (!on) {
        Icon(Icons.Filled.Tune, contentDescription = description)
        return
    }
    BadgedBox(badge = { Badge(containerColor = MaterialTheme.colorScheme.tertiary) }) {
        Icon(Icons.Filled.Tune, contentDescription = description, tint = MaterialTheme.colorScheme.primary)
    }
}
