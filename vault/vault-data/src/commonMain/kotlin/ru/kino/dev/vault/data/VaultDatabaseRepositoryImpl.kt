package ru.kino.dev.vault.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import ru.kino.dev.core.CoreException
import ru.kino.dev.core.DatabaseFiles
import ru.kino.dev.core.MergeSummary
import ru.kino.dev.core.NativeCore
import ru.kino.dev.vault.VaultDatabaseRepository
import ru.kino.dev.vault.data.changes.VaultChangesImpl
import ru.kino.dev.vault.data.sync.mergeCall
import ru.kino.dev.vault.sync.SaveStatus

/**
 * [VaultDatabaseRepository] over the native core and the document the database lives in.
 *
 * The core produces bytes and forgets about them; whether they reached the file is decided here, and a
 * write that half happened is the one failure that loses a database - so writes are shielded from
 * cancellation.
 *
 * A file written elsewhere since it was read is never written over: the save leaves the change in memory and
 * [status] says so - the one place every save of `vault` goes through (pass-docs, Step 108).
 */
internal class VaultDatabaseRepositoryImpl(
    private val core: NativeCore,
    private val files: DatabaseFiles,
    private val changes: VaultChangesImpl,
    private val status: SaveStatus,
) : VaultDatabaseRepository {

    override suspend fun save(dbKey: String) {
        if (changedElsewhere(dbKey)) {
            status.changedElsewhere(dbKey)
            return
        }
        try {
            write(dbKey)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            status.notSaved(dbKey)
            throw e
        }
        status.saved(dbKey)
    }

    // A file that cannot be read now is not taken for written elsewhere: the write is tried as always, and fails
    // as always when the file cannot be reached
    private suspend fun changedElsewhere(dbKey: String): Boolean = try {
        hasChangedElsewhere(dbKey)
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        false
    }

    private suspend fun write(dbKey: String) {
        val bytes = core.saveDatabase(dbKey)
        // Cancelling the coroutine while the bytes are on their way to the file would leave a truncated
        // database, and the user cancelled a screen, not their data
        withContext(NonCancellable) {
            files.writeReplacing(dbKey, bytes)
            // Only once the bytes are in the file are they the file: otherwise the next check would take our
            // own save for somebody else's (pass-docs, Step 107)
            core.setFileChecksum(dbKey, bytes)
        }
    }

    override suspend fun lock(dbKey: String) {
        core.lockDatabase(dbKey)
    }

    override suspend fun close(dbKey: String) {
        core.closeDatabase(dbKey)
        status.forget(dbKey)
    }

    override suspend fun hasChangedElsewhere(dbKey: String): Boolean =
        try {
            core.verifyFileChecksum(dbKey, files.read(dbKey))
            false
        } catch (e: CoreException) {
            // The one kind that is an answer rather than a failure: the file is fine, it is just not ours
            // any more. Anything else - the database is not open, the bytes are not a kdbx - is a failure
            if (e.kind == FILE_CHANGED) true else throw e
        }

    override suspend fun merge(dbKey: String): MergeSummary = mergeCall {
        core.mergeDatabase(dbKey, files.read(dbKey)).also { changes.changed(dbKey) }
    }

    private companion object {
        /** The core's error variant for a file that no longer matches the checksum it was read with. */
        const val FILE_CHANGED = "DbFileContentChangeDetected"
    }
}
