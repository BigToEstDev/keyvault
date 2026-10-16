package ru.kino.dev.vault.history

import ru.kino.dev.vault.entry.EntryFailure
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
        return entry.history.mapIndexed { index, savedAt -> HistoryVersion(index, savedAt) }
    }
}
