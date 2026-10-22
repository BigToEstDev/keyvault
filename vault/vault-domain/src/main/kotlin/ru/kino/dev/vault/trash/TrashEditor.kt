package ru.kino.dev.vault.trash

import ru.kino.dev.vault.VaultDatabaseRepository
import ru.kino.dev.vault.group.EntryId
import ru.kino.dev.vault.group.GroupId
import ru.kino.dev.vault.lock.DatabaseGuard
import kotlin.coroutines.cancellation.CancellationException

/**
 * Changing the recycle bin: one action, one write of the file (pass-docs, Step 70). The same value of the switch
 * again, or nothing picked, is not a change: nothing is written.
 *
 * Each change runs inside [guard], so a lock waits for its write to end.
 *
 * Throws [TrashFailure] and nothing else.
 */
class TrashEditor(
    private val trash: TrashRepository,
    private val database: VaultDatabaseRepository,
    private val guard: DatabaseGuard = DatabaseGuard(),
) {

    /** Switches the bin on or off and saves. Off, the bin goes with all it holds - the screen asks first. */
    suspend fun setEnabled(dbKey: String, enabled: Boolean): Unit = guard.exclusive {
        if (trash.enabled(dbKey) == enabled) return@exclusive
        trash.setEnabled(dbKey, enabled)
        save(dbKey)
    }

    /** Moves [entryIds] out of the bin into [groupId] and saves. */
    suspend fun restore(dbKey: String, entryIds: List<EntryId>, groupId: GroupId): Unit = guard.exclusive {
        if (entryIds.isEmpty()) return@exclusive
        trash.restore(dbKey, entryIds, groupId)
        save(dbKey)
    }

    /** Removes [entryIds] for good and saves. */
    suspend fun remove(dbKey: String, entryIds: List<EntryId>): Unit = guard.exclusive {
        if (entryIds.isEmpty()) return@exclusive
        trash.remove(dbKey, entryIds)
        save(dbKey)
    }

    /** Removes everything in the bin for good and saves. */
    suspend fun empty(dbKey: String): Unit = guard.exclusive {
        trash.empty(dbKey)
        save(dbKey)
    }

    private suspend fun save(dbKey: String) {
        try {
            database.save(dbKey)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw TrashFailure.NotSaved(e)
        }
    }
}
