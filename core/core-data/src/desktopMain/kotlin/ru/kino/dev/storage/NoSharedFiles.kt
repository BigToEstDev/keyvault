package ru.kino.dev.storage

import ru.kino.dev.core.SharedFiles

/**
 * The desktop hands no file to another app (pass-docs, Step 111): a share menu is reached from the jvm only by native
 * code of each system, and Linux has none. "Save a copy" is there instead - the file dragged into a messenger after.
 */
internal class NoSharedFiles : SharedFiles {

    override val isAvailable: Boolean = false

    override suspend fun prepare(name: String, bytes: ByteArray): String =
        throw UnsupportedOperationException("the desktop shares no files")

    override suspend fun clear() = Unit
}
