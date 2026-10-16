package ru.kino.dev.vault.history

import ru.kino.dev.vault.VaultDatabaseRepository
import ru.kino.dev.vault.entry.saveChange
import ru.kino.dev.vault.group.EntryId
import ru.kino.dev.vault.lock.DatabaseGuard

/**
 * Changing the history of an entry the way a person does it (pass-docs, Step 51, item 4): one action, one
 * write of the file - as [ru.kino.dev.vault.entry.EntryEditor] does, inside [guard], so a lock waits for it.
 *
 * A version comes back as it was, unchecked: it is a state the file held, not one typed here.
 *
 * Throws [ru.kino.dev.vault.entry.EntryFailure] and nothing else.
 */
class EntryHistoryEditor(
    private val history: EntryHistoryRepository,
    private val database: VaultDatabaseRepository,
    private val guard: DatabaseGuard = DatabaseGuard(),
) {

    /** Makes the version at [index] the entry again - the current state goes into the history - and saves. */
    suspend fun restore(dbKey: String, entryId: EntryId, index: Int): Unit = guard.exclusive {
        history.restore(dbKey, entryId, index)
        database.saveChange(dbKey)
    }

    /** Deletes the version at [index] and saves. */
    suspend fun delete(dbKey: String, entryId: EntryId, index: Int): Unit = guard.exclusive {
        history.delete(dbKey, entryId, index)
        database.saveChange(dbKey)
    }
}
