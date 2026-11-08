package ru.kino.dev.core

/**
 * Files handed to another app - a messenger, the mail - through the share menu of the system (pass-docs, Step 111).
 *
 * A file is put into a folder of the app's own that is emptied before each new one and at the start of the app: when
 * the other app is done reading it is never told.
 */
interface SharedFiles {

    /** Whether the platform hands files to other apps - android does, the desktop does not. */
    val isAvailable: Boolean

    /**
     * Puts [bytes] under [name] into the folder - emptied first - and returns the uri another app reads it by.
     *
     * @throws DatabaseFileException the file could not be put there - no room, say
     * @throws UnsupportedOperationException not [isAvailable]
     */
    suspend fun prepare(name: String, bytes: ByteArray): String

    /** Empties the folder - at the start of the app. Never fails: what stays is removed the next time. */
    suspend fun clear()
}
