package ru.kino.dev.vault.entry

import ru.kino.dev.vault.group.EntryId
import ru.kino.dev.vault.group.GroupId

/**
 * [EntryRepository] in memory, for tests of what is built on it: the entries change the way the core
 * changes them, and every call is written to [calls] - share it to check the order against a save.
 *
 * A failure is set per method in [failures] and thrown each time that method is called, after it is
 * recorded.
 */
class FakeEntryRepository(val calls: MutableList<String> = mutableListOf()) : EntryRepository {

    /** An entry the fake holds: the group it is in, its fields in order, when it was made. */
    data class StoredEntry(val groupId: GroupId, val fields: List<EntryField>, val createdAt: Long)

    /** Method name to the failure it throws. */
    val failures = mutableMapOf<String, Exception>()

    val stored = LinkedHashMap<EntryId, StoredEntry>()

    /** Entries moved into the recycle bin, in order. */
    val recycled = mutableListOf<EntryId>()

    private var created = 0

    /** The creation time handed to the next entry made without one, in seconds. */
    private var clock = 1_700_000_000L

    /**
     * Puts an entry into [groupId], for setting a test up. Not recorded. Without [createdAt] every entry is
     * a second younger than the one put before it.
     */
    fun add(id: String, groupId: GroupId, fields: List<EntryField>, createdAt: Long = ++clock): EntryId =
        EntryId(id).also { stored[it] = StoredEntry(groupId, fields, createdAt) }

    override suspend fun entries(dbKey: String, groupId: GroupId): List<EntrySummary> {
        call("entries")
        return stored.filterValues { it.groupId == groupId }
            .map { (id, entry) -> entry.summary(id) }
            .sortedBy { it.title }
    }

    /** By the rules of the core: title or user name, case aside, never a protected field. */
    override suspend fun search(dbKey: String, term: String): EntrySearch {
        call("search")
        val needle = term.trim()
        if (needle.isEmpty()) return EntrySearch(term, emptyList())
        val found = stored
            .filterValues { entry ->
                listOf(StandardPreset.TITLE, StandardPreset.USER_NAME).any { key ->
                    entry.fields.firstOrNull { it.key == key && !it.protected }
                        ?.value?.contains(needle, ignoreCase = true) == true
                }
            }
            .map { (id, entry) -> entry.summary(id) }
        return EntrySearch(term, found)
    }

    override suspend fun fields(dbKey: String, entryId: EntryId): List<EntryField> {
        call("fields")
        return entry(entryId).fields
    }

    override suspend fun create(dbKey: String, groupId: GroupId, fields: List<EntryField>): EntryId {
        call("create")
        refuseDuplicateKeys(fields)
        return add("entry-${++created}", groupId, fields)
    }

    override suspend fun update(dbKey: String, entryId: EntryId, fields: List<EntryField>) {
        call("update")
        val entry = entry(entryId)
        refuseDuplicateKeys(fields)
        stored[entryId] = entry.copy(fields = fields)
    }

    override suspend fun moveToRecycleBin(dbKey: String, entryId: EntryId) {
        call("moveToRecycleBin")
        entry(entryId)
        stored.remove(entryId)
        recycled += entryId
    }

    private fun entry(id: EntryId) = stored[id] ?: throw EntryFailure.EntryNotFound()

    private fun StoredEntry.value(key: String) = fields.firstOrNull { it.key == key }?.value.orEmpty()

    private fun StoredEntry.summary(id: EntryId) = EntrySummary(
        id = id,
        groupId = groupId,
        title = value(StandardPreset.TITLE),
        userName = value(StandardPreset.USER_NAME),
        createdAt = createdAt,
    )

    // The core refuses two fields under one key, and the data layer reports that as a bug of ours
    private fun refuseDuplicateKeys(fields: List<EntryField>) {
        if (fields.map { it.key }.toSet().size != fields.size) {
            throw EntryFailure.Unexpected(IllegalArgumentException("two fields with one key"))
        }
    }

    private fun call(method: String) {
        calls += method
        failures[method]?.let { throw it }
    }
}
