package ru.kino.dev.storage

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.kino.dev.core.DatabaseFileException
import ru.kino.dev.core.DatabaseFiles
import java.io.IOException
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import kotlin.io.path.isRegularFile

/**
 * [DatabaseFiles] on the file system of the desktop: the uri is `file:`, as the picker of the desktop hands it back.
 * Nothing to keep access to - a path stays readable after a restart, as far as the system allows.
 */
internal class FileDatabaseFiles(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : DatabaseFiles {

    override suspend fun read(uri: String): ByteArray = io(uri) { Files.readAllBytes(it) }

    override suspend fun readAtMost(uri: String, maxBytes: Int): ByteArray = io(uri) { path ->
        Files.newInputStream(path).use { it.readNBytes(maxBytes) }
    }

    override suspend fun size(uri: String): Long? = io(uri) { it.fileSize() }

    override suspend fun isEmpty(uri: String): Boolean = io(uri) { !it.exists() || it.fileSize() == 0L }

    override suspend fun delete(uri: String): Boolean = withContext(dispatcher) {
        runCatching { Files.deleteIfExists(pathOf(uri)) }.getOrDefault(false)
    }

    override suspend fun writeReplacing(uri: String, bytes: ByteArray) = io(uri) {
        Files.write(it, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)
        Unit
    }

    override suspend fun exists(uri: String): Boolean = withContext(dispatcher) {
        runCatching { pathOf(uri).isRegularFile() }.getOrDefault(false)
    }

    override suspend fun displayName(uri: String): String? = runCatching { pathOf(uri).fileName?.toString() }.getOrNull()

    override suspend fun keepAccess(uri: String, forWriting: Boolean) = Unit

    private suspend fun <T> io(uri: String, block: (Path) -> T): T = withContext(dispatcher) {
        try {
            block(pathOf(uri))
        } catch (e: IOException) {
            throw DatabaseFileException("File io failed", e)
        } catch (e: IllegalArgumentException) {
            throw DatabaseFileException("Not a file uri", e)
        }
    }

    private fun pathOf(uri: String): Path = Path.of(URI(uri))
}
