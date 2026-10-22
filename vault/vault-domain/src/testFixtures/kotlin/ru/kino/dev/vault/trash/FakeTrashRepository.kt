package ru.kino.dev.vault.trash

import ru.kino.dev.vault.group.EntryId
import ru.kino.dev.vault.group.GroupId

/**
 * [TrashRepository] in memory, for tests of what is built on it: the bin changes the way the core changes it, and
 * every call is written to [calls].
 *
 * A failure is set per method in [failures] and thrown each time that method is called, after it is recorded.
 */
class FakeTrashRepository(var enabled: Boolean = true) : TrashRepository {

    val calls = mutableListOf<String>()

    /** Method name to the failure it throws. */
    val failures = mutableMapOf<String, Exception>()

    /** What the bin holds, in the order it was put there. */
    val entries = mutableListOf<TrashEntry>()

    /** The entries [restore] moved out, to the group they went to. */
    val restored = mutableMapOf<EntryId, GroupId>()

    override suspend fun enabled(dbKey: String): Boolean {
        call("enabled")
        return enabled
    }

    // Like the core: switched off, the bin goes with all it holds
    override suspend fun setEnabled(dbKey: String, enabled: Boolean) {
        call("setEnabled")
        if (!enabled) entries.clear()
        this.enabled = enabled
    }

    override suspend fun entries(dbKey: String): List<TrashEntry> {
        call("entries")
        return entries.toList()
    }

    override suspend fun search(dbKey: String, term: String): TrashSearch {
        call("search")
        val needle = term.trim()
        if (needle.isEmpty()) return TrashSearch(term, emptyList())
        val found = entries.filter { item ->
            listOf(item.entry.title, item.entry.userName).any { it.contains(needle, ignoreCase = true) }
        }
        return TrashSearch(term, found)
    }

    override suspend fun restore(dbKey: String, entryIds: List<EntryId>, groupId: GroupId) {
        call("restore")
        refuseOutsideBin(entryIds)
        entryIds.forEach { restored[it] = groupId }
        entries.removeAll { it.entry.id in entryIds }
    }

    override suspend fun remove(dbKey: String, entryIds: List<EntryId>) {
        call("remove")
        refuseOutsideBin(entryIds)
        entries.removeAll { it.entry.id in entryIds }
    }

    override suspend fun empty(dbKey: String) {
        call("empty")
        entries.clear()
    }

    // Like the core: all or nothing
    private fun refuseOutsideBin(entryIds: List<EntryId>) {
        val held = entries.map { it.entry.id }.toSet()
        if (!held.containsAll(entryIds)) throw TrashFailure.Stale()
    }

    private fun call(method: String) {
        calls += method
        failures[method]?.let { throw it }
    }
}
