package ru.kino.dev.vault.history

import ru.kino.dev.vault.group.EntryId

/**
 * The history of the entries of the open database: kdbx keeps the old versions of an entry inside it, and
 * the core puts the previous state there on every update.
 *
 * Throws [ru.kino.dev.vault.entry.EntryFailure] and nothing else - the history is a part of the entry.
 */
interface EntryHistoryRepository {

    /** The old versions of the entry, oldest first, as the core keeps them; the current state is not one. */
    suspend fun versions(dbKey: String, entryId: EntryId): List<HistoryVersion>
}

/**
 * One old version of an entry.
 *
 * @param index its position in the history, oldest first - not an identity: deleting a version moves the
 *   rest, so the history is read again after every change
 * @param modified when this version was saved, in seconds since the epoch, UTC; null - the time could not
 *   be read
 */
data class HistoryVersion(val index: Int, val modified: Long?)
