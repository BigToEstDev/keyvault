package ru.kino.dev.vault.filter

import ru.kino.dev.vault.entry.EntrySummary
import ru.kino.dev.vault.group.VaultGroup

/** What the list shows: the groups first, then the entries. */
data class VaultListing(
    val groups: List<VaultGroup>,
    val entries: List<EntrySummary>,
)

/**
 * The [groups] and [entries] as this filter shows them: only what [VaultFilter.visibility] lets through,
 * the groups in the order they came, the entries by when they were created - those made in the same second
 * by title, case aside.
 */
fun VaultFilter.applyTo(groups: List<VaultGroup>, entries: List<EntrySummary>): VaultListing {
    val byTitle = compareBy(String.CASE_INSENSITIVE_ORDER, EntrySummary::title)
    val byTime = when (order) {
        CreationOrder.NewestFirst -> compareByDescending<EntrySummary> { it.createdAt }
        CreationOrder.OldestFirst -> compareBy<EntrySummary> { it.createdAt }
    }
    return VaultListing(
        groups = if (visibility == Visibility.EntriesOnly) emptyList() else groups,
        entries = if (visibility == Visibility.GroupsOnly) emptyList() else entries.sortedWith(byTime.then(byTitle)),
    )
}
