package ru.kino.dev.vault.filter

import ru.kino.dev.vault.entry.EntrySummary

/** What the list shows: the groups first - as the caller holds them, [G] - then the entries. */
data class VaultListing<G>(
    val groups: List<G>,
    val entries: List<EntrySummary>,
)

/**
 * The [groups] and [entries] as this filter shows them: only what [VaultFilter.visibility] lets through,
 * the groups in the order they came, the entries by when they were created - those made in the same second
 * by title, case aside.
 */
fun <G> VaultFilter.applyTo(groups: List<G>, entries: List<EntrySummary>): VaultListing<G> {
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
