package ru.kino.dev.vault.data.sync

import kotlinx.coroutines.CancellationException
import ru.kino.dev.core.DatabaseFiles
import ru.kino.dev.core.SharedFiles
import ru.kino.dev.vault.sync.DatabaseCopyFailure
import ru.kino.dev.vault.sync.DatabaseCopyRepository

/**
 * [DatabaseCopyRepository] on the documents (pass-docs, Step 111): the file of the database read and written as bytes -
 * no core: a copy is the same file - and handed to other apps through [SharedFiles] of the platform.
 */
internal class DatabaseCopyRepositoryImpl(
    private val documents: DatabaseFiles,
    private val shared: SharedFiles,
) : DatabaseCopyRepository {

    override suspend fun read(dbKey: String): ByteArray = failing(DatabaseCopyFailure::Unreadable) {
        documents.read(dbKey)
    }

    override suspend fun write(uri: String, bytes: ByteArray) = failing(DatabaseCopyFailure::NotWritten) {
        documents.writeReplacing(uri, bytes)
    }

    override suspend fun name(dbKey: String): String? = try {
        documents.displayName(dbKey)
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

    override fun isShareAvailable(): Boolean = shared.isAvailable

    override suspend fun prepareShare(name: String, bytes: ByteArray): String =
        failing(DatabaseCopyFailure::NotWritten) { shared.prepare(name, bytes) }

    private suspend fun <T> failing(failure: (Throwable) -> DatabaseCopyFailure, block: suspend () -> T): T = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        throw failure(e)
    }
}
