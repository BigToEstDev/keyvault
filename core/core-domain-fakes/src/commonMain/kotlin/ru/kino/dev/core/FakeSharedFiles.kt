package ru.kino.dev.core

/** [SharedFiles] in memory: what was put to share, and a failure to throw instead. */
class FakeSharedFiles : SharedFiles {

    override var isAvailable: Boolean = true

    /** The files put to share, by name - emptied by each [prepare], as the real folder is. */
    val files = mutableMapOf<String, ByteArray>()

    /** Thrown by [prepare] instead of putting the file there. */
    var failure: Exception? = null

    override suspend fun prepare(name: String, bytes: ByteArray): String {
        failure?.let { throw it }
        files.clear()
        files[name] = bytes.copyOf()
        return "$URI_PREFIX$name"
    }

    override suspend fun clear() = files.clear()

    companion object {
        /** What [prepare] puts before the name in the uri it hands back. */
        const val URI_PREFIX = "content://shared/"
    }
}
