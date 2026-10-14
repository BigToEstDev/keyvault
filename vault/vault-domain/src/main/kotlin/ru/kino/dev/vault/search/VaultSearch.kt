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
    /**
     * Where what is found lies: for each group holding an entry found - its [VaultSearchResult.entries] by
     * `groupId` - or a group found - by `parentId` - the names from the group searched in down to it, both
     * included. Two of one name are told apart by it.
     */
    val locations: Map<GroupId, List<String>>,
)

/**
 * The search of the list: everything under a group - at the root, the whole database - found by a term
 * (pass-docs, `docs/android/screens/vault/vault-our.md`, "Поиск").
 *
 * A group is found by its name, an entry by its title or its user name; as plain text, case aside. Each
 * comes with where it lies, from the group searched in (pass-docs, Step 40, item 10). The
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
        if (needle.isEmpty()) return VaultSearchResult(term, emptyList(), emptyList(), emptyMap(), emptyMap())

        val tree = groups.tree(dbKey)
        val found = entries.search(dbKey, needle)
        val foundGroups = tree.descendants(scope).filter { it.name.contains(needle, ignoreCase = true) }
        val foundEntries = found.entries.filter { tree.isWithin(it.groupId, scope) }
        // A group found is below the scope, so it has a parent
        val holders = (foundEntries.map { it.groupId } + foundGroups.mapNotNull { it.parentId }).toSet()
        return VaultSearchResult(
            term = term,
            groups = foundGroups,
            entries = foundEntries,
            entryCounts = foundGroups.associate { it.id to tree.entryCount(it.id) },
            locations = holders.associateWith { holder -> tree.pathFrom(scope, holder).map { it.name } },
        )
    }
}
