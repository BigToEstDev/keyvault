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
import ru.kino.dev.vault.sync.OtherDatabaseRepository

/**
 * [OtherDatabaseRepository] on the core - `merge_kdbx_from_bytes` / `import_kdbx_from_bytes` (Step 106),
 * `merge_kdbx_both_ways` (Step 109) - and on the documents for the files picked. Neither file is kept: read once - and
 * both ways written once - the key file wiped after.
 */
internal class OtherDatabaseRepositoryImpl(
    private val core: NativeCore,
    private val documents: DatabaseFiles,
    private val changes: VaultChangesImpl,
) : OtherDatabaseRepository {

    override suspend fun mergeCopy(dbKey: String, uri: String, credentials: OtherDatabaseCredentials): MergeSummary =
        merge(dbKey, uri, credentials) { bytes, password, keyFile -> core.mergeCopy(dbKey, bytes, password, keyFile) }

    override suspend fun mergeCopyBothWays(
        dbKey: String,
        uri: String,
        credentials: OtherDatabaseCredentials,
    ): MergedBothWays =
        merge(dbKey, uri, credentials) { bytes, password, keyFile ->
            core.mergeCopyBothWays(dbKey, bytes, password, keyFile)
        }

    override suspend fun writeCopy(uri: String, bytes: ByteArray) {
        try {
            documents.writeReplacing(uri, bytes)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw MergeFailure.CopyNotWritten(e)
        }
    }

    override suspend fun importDatabase(
        dbKey: String,
        uri: String,
        credentials: OtherDatabaseCredentials,
    ): MergeSummary =
        merge(dbKey, uri, credentials) { bytes, password, keyFile ->
            core.importDatabase(dbKey, bytes, password, keyFile)
        }

    override suspend fun name(uri: String): String? = try {
        documents.displayName(uri)
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

    private suspend fun <T> merge(
        dbKey: String,
        uri: String,
        credentials: OtherDatabaseCredentials,
        call: suspend (ByteArray, String?, KeyFile?) -> T,
    ): T = mergeCall {
        val keyFile = credentials.keyFileUri?.let { keyFile(it) }
        try {
            val bytes = documents.read(uri)
            call(bytes, credentials.password, keyFile).also { changes.changed(dbKey) }
        } finally {
            keyFile?.wipe()
        }
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
