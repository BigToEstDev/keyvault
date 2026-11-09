package ru.kino.dev.vault.sync

import kotlinx.coroutines.CancellationException
import ru.kino.dev.core.MergeSummary
import ru.kino.dev.vault.VaultDatabaseRepository
import ru.kino.dev.vault.lock.DatabaseGuard
import ru.kino.dev.vault.lock.unstoppable

/**
 * Bringing another database file into the open one from the settings - a copy merged, a different one imported -
 * and writing the result: one action, one write (pass-docs, Step 108). A file of the open database written elsewhere
 * meanwhile is not written over: the save leaves the merge in memory, and the sync of `vault` asks.
 *
 * Both ways (Step 110) the copy's file is written too - after this database's, and only once it is written: a copy
 * ahead of the file it was merged into would leave the two files apart again.
 *
 * The file of the open database itself is refused before anything changes, on every kind. The other file is read
 * first, outside [guard] - it has nothing to do with the open database, and the read goes with its screen; the merge
 * and the write of this database run inside it, to the end, so a lock waits for them; the copy's file, both ways, is
 * written after it - a lock does not wait for another provider (pass-docs, Step 114).
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
        val file = other.read(uri, credentials)
        try {
            return when (kind) {
                OtherDatabaseKind.Copy -> guard.exclusiveChange { other.mergeCopy(dbKey, file).also { save(dbKey) } }
                OtherDatabaseKind.Import -> guard.exclusiveChange { other.importDatabase(dbKey, file).also { save(dbKey) } }
                OtherDatabaseKind.CopyBothWays -> bothWays(dbKey, uri, file)
            }
        } finally {
            file.wipe()
        }
    }

    private suspend fun bothWays(dbKey: String, uri: String, file: OtherDatabaseFile): MergeSummary {
        val merged = guard.exclusiveChange {
            other.mergeCopyBothWays(dbKey, file).also {
                save(dbKey)
                // Not written - the file was saved elsewhere: the sync asks; the copy waits for this database
                if (status.current(dbKey) != SaveState.Saved) throw MergeFailure.CopyNotWrittenChangedElsewhere()
            }
        }
        // Begun, the merge goes into both files: the copy's write runs to the end too
        unstoppable { other.writeCopy(uri, merged.copy) }
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
