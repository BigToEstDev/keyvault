package ru.kino.dev.storage

import android.content.Context
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.kino.dev.core.DatabaseFileException
import ru.kino.dev.core.SharedFiles

/**
 * [SharedFiles] in `cache/shared` of the app, handed out by the `FileProvider` of the manifest of `:app`
 * (`res/xml/file_paths.xml`): the other app gets a `content:` uri and a grant to read it, never the path.
 */
internal class AndroidSharedFiles(
    private val context: Context,
) : SharedFiles {

    private val folder: File get() = File(context.cacheDir, FOLDER)

    override val isAvailable: Boolean = true

    override suspend fun prepare(name: String, bytes: ByteArray): String = withContext(Dispatchers.IO) {
        try {
            empty()
            val folder = folder.apply { mkdirs() }
            // A name from a provider is not a path: whatever looks like a folder in it is cut off
            val file = File(folder, File(name).name.ifEmpty { FALLBACK_NAME })
            file.writeBytes(bytes)
            FileProvider.getUriForFile(context, "${context.packageName}$AUTHORITY_SUFFIX", file).toString()
        } catch (e: IOException) {
            throw DatabaseFileException("cannot put the file to share", e)
        } catch (e: IllegalArgumentException) {
            // The folder is not in file_paths.xml - a bug of the build, said as a write that failed
            throw DatabaseFileException("the file to share is outside the provider", e)
        }
    }

    override suspend fun clear() = withContext(Dispatchers.IO) { empty() }

    private fun empty() {
        folder.listFiles()?.forEach { it.delete() }
    }

    private companion object {
        /** The folder in the cache - `cache-path` of `file_paths.xml`. */
        const val FOLDER = "shared"

        /** After the package name - `android:authorities` of the provider in the manifest of `:app`. */
        const val AUTHORITY_SUFFIX = ".files"

        const val FALLBACK_NAME = "keyvault.kdbx"
    }
}
