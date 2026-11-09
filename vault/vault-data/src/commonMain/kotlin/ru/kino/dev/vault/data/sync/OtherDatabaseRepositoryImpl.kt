package ru.kino.dev.vault.data.sync

import kotlinx.coroutines.CancellationException
import ru.kino.dev.core.CoreException
import ru.kino.dev.core.DatabaseFileException
import ru.kino.dev.core.DatabaseFiles
import ru.kino.dev.core.KeyFile
import ru.kino.dev.core.MergeSummary
import ru.kino.dev.core.MergedBothWays
import ru.kino.dev.core.NativeCore
import ru.kino.dev.core.readKeyFile
import ru.kino.dev.vault.data.changes.VaultChangesImpl
import ru.kino.dev.vault.sync.MergeFailure
import ru.kino.dev.vault.sync.OtherDatabaseCredentials
import ru.kino.dev.vault.sync.OtherDatabaseFile
import ru.kino.dev.vault.sync.OtherDatabaseRepository

/**
 * [OtherDatabaseRepository] on the core - `merge_kdbx_from_bytes` / `import_kdbx_from_bytes` (Step 106),
 * `merge_kdbx_both_ways` (Step 109) - and on the documents for the files picked. Neither file is kept: read once - and
 * both ways written once; the key file is wiped by the caller, once the merge is over.
 */
internal class OtherDatabaseRepositoryImpl(
    private val core: NativeCore,
    private val documents: DatabaseFiles,
    private val changes: VaultChangesImpl,
) : OtherDatabaseRepository {

    override suspend fun read(uri: String, credentials: OtherDatabaseCredentials): OtherDatabaseFile = mergeCall {
        val keyFile = credentials.keyFileUri?.let { keyFile(it) }
        val bytes = try {
            documents.read(uri)
        } catch (e: Throwable) {
            keyFile?.wipe()
            throw e
        }
        OtherDatabaseFile(bytes, credentials.password, keyFile)
    }

    override suspend fun mergeCopy(dbKey: String, file: OtherDatabaseFile): MergeSummary =
        merge(dbKey) { core.mergeCopy(dbKey, file.bytes, file.password, file.keyFile) }

    override suspend fun mergeCopyBothWays(dbKey: String, file: OtherDatabaseFile): MergedBothWays =
        merge(dbKey) { core.mergeCopyBothWays(dbKey, file.bytes, file.password, file.keyFile) }

    override suspend fun writeCopy(uri: String, bytes: ByteArray) {
        try {
            documents.writeReplacing(uri, bytes)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw MergeFailure.CopyNotWritten(e)
        }
    }

    override suspend fun importDatabase(dbKey: String, file: OtherDatabaseFile): MergeSummary =
        merge(dbKey) { core.importDatabase(dbKey, file.bytes, file.password, file.keyFile) }

    override suspend fun name(uri: String): String? = try {
        documents.displayName(uri)
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

    private suspend fun <T> merge(dbKey: String, call: suspend () -> T): T = mergeCall {
        call().also { changes.changed(dbKey) }
    }

    // Its own failures: a key file that cannot be read is not the database file that cannot be read
    private suspend fun keyFile(uri: String): KeyFile = try {
        documents.readKeyFile(uri)
    } catch (e: DatabaseFileException) {
        throw MergeFailure.KeyFileUnreadable(e)
    } catch (e: CoreException) {
        throw e.toMergeFailure()
    }
}
