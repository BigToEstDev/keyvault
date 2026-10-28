package ru.kino.dev.vault.history

import ru.kino.dev.core.preset.StandardPreset
import ru.kino.dev.vault.entry.EntryRepository
import ru.kino.dev.vault.group.EntryId

/**
 * The history of an entry as its screen shows it (pass-docs, Step 51, item 3): the name of the entry, its
 * old versions newest first, and when it was made - the last card.
 *
 * A version whose time cannot be read is left out: its card would have nothing to say (owner, 17.10.26).
 * When the entry was made is the entry's own time, not a version: the history is trimmed to its limits, and
 * the very first version may be gone.
 *
 * Throws [ru.kino.dev.vault.entry.EntryFailure] and nothing else.
 */
class EntryHistoryReader(
    private val entries: EntryRepository,
    private val history: EntryHistoryRepository,
) {

    suspend fun read(dbKey: String, entryId: EntryId): EntryHistory {
        val record = entries.read(dbKey, entryId)
        val versions = history.versions(dbKey, entryId)
        return EntryHistory(
            title = record.fields.firstOrNull { it.key == StandardPreset.TITLE }?.value.orEmpty(),
            versions = versions.filter { it.modified != null }.sortedByDescending { it.index },
            created = record.times.created,
        )
    }
}

/**
 * @param title of the entry now; empty when it has none
 * @param versions newest first, each with its time
 * @param created when the entry was made, in seconds since the epoch, UTC; null - it could not be read
 */
data class EntryHistory(val title: String, val versions: List<HistoryVersion>, val created: Long?)
