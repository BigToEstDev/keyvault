package ru.kino.dev.vault.history

import ru.kino.dev.vault.entry.EntryFailure
import ru.kino.dev.vault.entry.EntryRecord
import ru.kino.dev.vault.entry.EntryTimes
import ru.kino.dev.vault.entry.FakeEntryRepository
import ru.kino.dev.vault.group.EntryId

/**
 * [EntryHistoryRepository] over the entries of [entries]: the history of an entry is what its updates there
 * left behind ([FakeEntryRepository.StoredEntry.history]).
 *
 * A failure is set per method in [failures] and thrown each time that method is called.
 */
class FakeEntryHistoryRepository(private val entries: FakeEntryRepository) : EntryHistoryRepository {

    /** Method name to the failure it throws. */
    val failures = mutableMapOf<String, Exception>()

    override suspend fun versions(dbKey: String, entryId: EntryId): List<HistoryVersion> {
        failures["versions"]?.let { throw it }
        val entry = entries.stored[entryId] ?: throw EntryFailure.EntryNotFound()
        return entry.history.mapIndexed { index, version -> HistoryVersion(index, version.savedAt) }
    }

    override suspend fun version(dbKey: String, entryId: EntryId, index: Int): EntryRecord {
        failures["version"]?.let { throw it }
        val entry = entries.stored[entryId] ?: throw EntryFailure.EntryNotFound()
        val version = entry.history.getOrNull(index) ?: throw EntryFailure.EntryNotFound()
        return EntryRecord(entry.groupId, version.fields, EntryTimes(entry.createdAt, version.savedAt), version.attachments)
    }

    override suspend fun restore(dbKey: String, entryId: EntryId, index: Int) {
        failures["restore"]?.let { throw it }
        val entry = entries.stored[entryId] ?: throw EntryFailure.EntryNotFound()
        val version = entry.history.getOrNull(index) ?: throw EntryFailure.EntryNotFound()
        entries.stored[entryId] = entry.updatedTo(version.fields, entries.tick(), version.attachments)
    }

    override suspend fun delete(dbKey: String, entryId: EntryId, index: Int) {
        failures["delete"]?.let { throw it }
        val entry = entries.stored[entryId] ?: throw EntryFailure.EntryNotFound()
        if (index !in entry.history.indices) throw EntryFailure.EntryNotFound()
        entries.stored[entryId] = entry.copy(history = entry.history.filterIndexed { i, _ -> i != index })
    }

    override suspend fun deleteAll(dbKey: String, entryId: EntryId) {
        failures["deleteAll"]?.let { throw it }
        val entry = entries.stored[entryId] ?: throw EntryFailure.EntryNotFound()
        entries.stored[entryId] = entry.copy(history = emptyList())
    }
}
