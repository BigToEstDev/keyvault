package ru.kino.dev.vault.search

import ru.kino.dev.vault.entry.EntryRepository
import ru.kino.dev.vault.entry.EntrySummary
import ru.kino.dev.vault.group.GroupId
import ru.kino.dev.vault.group.GroupRepository
import ru.kino.dev.vault.group.VaultGroup

/** What a term finds under a group, with the term itself; in no particular order - the filter sorts. */
data class VaultSearchResult(
    val term: String,
    /** In the order of the tree: each right after the group it is in */
    val groups: List<VaultGroup>,
    val entries: List<EntrySummary>,
    /** For each group found: the entries in it and in every group below it - what its card shows */
    val entryCounts: Map<GroupId, Int>,
)

/**
 * The search of the list: everything under a group - at the root, the whole database - found by a term
 * (pass-docs, `docs/android/screens/vault/vault-our.md`, "Поиск").
 *
 * A group is found by its name, an entry by its title or its user name; as plain text, case aside. The
 * entries are the core's to find - only it knows which field is protected - and are kept when they lie
 * under the group; the groups are found here, in the tree. The recycle bin is never searched: the tree does
 * not show it and the core does not look into it.
 *
 * Throws what the repositories throw: [ru.kino.dev.vault.group.GroupFailure] and
 * [ru.kino.dev.vault.entry.EntryFailure].
 */
class VaultSearch(
    private val groups: GroupRepository,
    private val entries: EntryRepository,
) {

    suspend fun search(dbKey: String, scope: GroupId, term: String): VaultSearchResult {
        val needle = term.trim()
        if (needle.isEmpty()) return VaultSearchResult(term, emptyList(), emptyList(), emptyMap())

        val tree = groups.tree(dbKey)
        val found = entries.search(dbKey, needle)
        val foundGroups = tree.descendants(scope).filter { it.name.contains(needle, ignoreCase = true) }
        return VaultSearchResult(
            term = term,
            groups = foundGroups,
            entries = found.entries.filter { tree.isWithin(it.groupId, scope) },
            entryCounts = foundGroups.associate { it.id to tree.entryCount(it.id) },
        )
    }
}
