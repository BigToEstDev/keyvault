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
 * Both ways (Step 110) the copy's file is written too - after this database's, and only once it is written: a copy
 * ahead of the file it was merged into would leave the two files apart again.
 *
 * The file of the open database itself is refused before anything changes, on every kind. Runs inside [guard], so a
 * lock waits for the merge and its writes.
 *
 * Throws [MergeFailure] and nothing else: `NotSaved` for a merge made but not written; both ways - `CopyNotWritten` /
 * `CopyNotWrittenChangedElsewhere` for a merge written into this database only.
 */
class OtherDatabaseEditor(
    private val other: OtherDatabaseRepository,
    private val database: VaultDatabaseRepository,
    private val status: SaveStatus,
    private val guard: DatabaseGuard,
) {

    suspend fun bringIn(
        dbKey: String,
        kind: OtherDatabaseKind,
        uri: String,
        credentials: OtherDatabaseCredentials,
    ): MergeSummary {
        // The same uri only: one file may come under different uris on android, not caught (pass-docs, Step 110)
        if (uri == dbKey) throw MergeFailure.OwnFile()
        // The merge reads the other file too, so all of it runs to its write - the read apart: Step 114 p2
        return guard.exclusiveChange {
            when (kind) {
                OtherDatabaseKind.Copy -> other.mergeCopy(dbKey, uri, credentials).also { save(dbKey) }
                OtherDatabaseKind.Import -> other.importDatabase(dbKey, uri, credentials).also { save(dbKey) }
                OtherDatabaseKind.CopyBothWays -> bothWays(dbKey, uri, credentials)
            }
        }
    }

    private suspend fun bothWays(dbKey: String, uri: String, credentials: OtherDatabaseCredentials): MergeSummary {
        val merged = other.mergeCopyBothWays(dbKey, uri, credentials)
        save(dbKey)
        // Not written - the file was saved elsewhere: the sync asks; the copy waits for this database
        if (status.current(dbKey) != SaveState.Saved) throw MergeFailure.CopyNotWrittenChangedElsewhere()
        other.writeCopy(uri, merged.copy)
        return merged.summary
    }

    private suspend fun save(dbKey: String) {
        try {
            database.save(dbKey)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw MergeFailure.NotSaved(e)
        }
    }
}
