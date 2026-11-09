package ru.kino.dev.storage

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File

/**
 * A provider of one document with the behaviour of a provider out there - one the app cannot test on: the
 * one that ignores the "t" of "wt" (pass-docs, Step 112).
 *
 * A pipe is not here: in Robolectric `createPipe` is a temporary file, so a provider handing a pipe cannot be
 * shown on the JVM.
 */
class TestDocumentProvider : ContentProvider() {

    enum class Behaviour {
        /** As local storage: "wt" truncates. */
        Truncates,

        /** Opens for writing and ignores the "t": the tail of a longer file stays. */
        IgnoresTruncate,
    }

    var behaviour: Behaviour = Behaviour.Truncates

    lateinit var file: File

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor = when {
        mode == "r" -> ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        behaviour == Behaviour.Truncates -> ParcelFileDescriptor.open(file, ParcelFileDescriptor.parseMode(mode))
        else -> ParcelFileDescriptor.open(file, WRITE_KEEPING_TAIL)
    }

    override fun onCreate(): Boolean = true

    override fun getType(uri: Uri): String = "application/octet-stream"

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

    private companion object {
        const val WRITE_KEEPING_TAIL = ParcelFileDescriptor.MODE_WRITE_ONLY or ParcelFileDescriptor.MODE_CREATE
    }
}
