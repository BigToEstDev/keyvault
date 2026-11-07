package ru.kino.dev.vault.sync

import kotlinx.coroutines.CancellationException
import ru.kino.dev.core.MergeSummary
import ru.kino.dev.vault.VaultDatabaseRepository
import ru.kino.dev.vault.lock.DatabaseGuard

/**
 * Bringing another database file into the open one from the settings - a copy merged, a different one imported -
 * and writing the result: one action, one write (pass-docs, Step 108). A file of the open database written elsewhere
 * meanwhile is not written over: the save leaves the merge in memory, and the sync of `vault` asks.
 *
 * Runs inside [guard], so a lock waits for the merge and its write.
 *
 * Throws [MergeFailure] and nothing else: `NotSaved` for a merge made but not written.
 */
class OtherDatabaseEditor(
    private val other: OtherDatabaseRepository,
    private val database: VaultDatabaseRepository,
    private val guard: DatabaseGuard,
) {

    suspend fun bringIn(
        dbKey: String,
        kind: OtherDatabaseKind,
        uri: String,
        credentials: OtherDatabaseCredentials,
    ): MergeSummary = guard.exclusive {
        val summary = when (kind) {
            OtherDatabaseKind.Copy -> other.mergeCopy(dbKey, uri, credentials)
            OtherDatabaseKind.Import -> other.importDatabase(dbKey, uri, credentials)
        }
        try {
            database.save(dbKey)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw MergeFailure.NotSaved(e)
        }
        summary
    }
}
