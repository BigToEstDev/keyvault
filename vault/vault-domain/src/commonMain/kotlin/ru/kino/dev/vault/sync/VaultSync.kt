package ru.kino.dev.vault.sync

import kotlinx.coroutines.CancellationException
import ru.kino.dev.core.MergeSummary
import ru.kino.dev.vault.VaultDatabaseRepository
import ru.kino.dev.vault.lock.DatabaseGuard

/**
 * Bringing a file written elsewhere into the open database, and writing the result (pass-docs, Step 108).
 *
 * A save never overwrites such a file: it leaves the change in memory and [SaveStatus] says
 * [SaveState.ChangedElsewhere]; [synchronize] is the answer "Synchronize" to that. [check] is the quiet one, on
 * coming back to the app and on unlocking.
 *
 * Both run inside [guard], so a lock waits for the merge and its write, and an edit waits for them too.
 */
class VaultSync(
    private val database: VaultDatabaseRepository,
    private val status: SaveStatus,
    private val guard: DatabaseGuard,
) {

    /**
     * Merges the file as it is now into the open database and saves, then reports what came in.
     *
     * Throws [MergeFailure]: `CredentialsChanged` for a file saved elsewhere with other credentials - merged
     * through the settings then; `NotSaved` for a merge made but not written. Nothing changed otherwise.
     */
    suspend fun synchronize(dbKey: String): MergeSummary = guard.exclusive {
        mergeAndSave(dbKey)
    }

    /**
     * Merges the file if it was written elsewhere and nothing of ours waits in memory - a change left after
     * "Cancel" or a failed write is not merged unasked. Null when nothing was done: the file is ours, there is
     * something of ours waiting, or the file cannot be read now - an offline provider is not a failure to report.
     *
     * Throws [MergeFailure] when the file was written elsewhere and cannot be merged - `CredentialsChanged` above
     * all; the ui says so.
     *
     * @param onMerging told once the file is known to be written elsewhere, before the merge - seconds of key
     * derivation; the check alone is quick and shows nothing
     */
    suspend fun check(dbKey: String, onMerging: suspend () -> Unit = {}): MergeSummary? = guard.exclusive {
        if (status.current(dbKey) != SaveState.Saved) return@exclusive null
        val changed = try {
            database.hasChangedElsewhere(dbKey)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            false
        }
        if (!changed) return@exclusive null
        onMerging()
        mergeAndSave(dbKey)
    }

    /**
     * Writes the database again after a write the file did not hold ([SaveState.Damaged]): the save does not ask
     * whether the file was written elsewhere then - the difference is ours (pass-docs, Step 112). Whether it is
     * written now; a write that fails again leaves [SaveState.Damaged] or says why in [status].
     */
    suspend fun saveAgain(dbKey: String): Boolean = guard.exclusive {
        try {
            database.save(dbKey)
            status.current(dbKey) == SaveState.Saved
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            false
        }
    }

    // The save too, not the merge alone: what is ours and was not in their file is in memory only until written
    private suspend fun mergeAndSave(dbKey: String): MergeSummary {
        val summary = database.merge(dbKey)
        try {
            database.save(dbKey)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw MergeFailure.NotSaved(e)
        }
        return summary
    }
}
