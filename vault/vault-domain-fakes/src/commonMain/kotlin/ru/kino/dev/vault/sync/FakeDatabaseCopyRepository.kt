package ru.kino.dev.vault.sync

/** The file of the open database copied: what was asked of it, and failures to throw instead. */
class FakeDatabaseCopyRepository : DatabaseCopyRepository {

    /** `"<read|write|prepareShare> <uri or name>"` for every read and write asked. */
    val calls = mutableListOf<String>()

    /** The bytes of the file of the database [read] hands back. */
    var fileBytes = byteArrayOf(4, 2)

    /** The name of the file of the database; null - the provider does not tell. */
    var fileName: String? = "base.kdbx"

    /** What [isShareAvailable] answers. */
    var shareAvailable = true

    /** The uri [prepareShare] hands back. */
    var shareUri = "content://shared/base.kdbx"

    /** The bytes the last [write] or [prepareShare] got. */
    var written: ByteArray? = null

    /** Thrown by [read] instead of reading. */
    var readFailure: DatabaseCopyFailure? = null

    /** Thrown by [write] and [prepareShare] instead of writing. */
    var writeFailure: DatabaseCopyFailure? = null

    override suspend fun read(dbKey: String): ByteArray {
        calls += "read $dbKey"
        readFailure?.let { throw it }
        return fileBytes.copyOf()
    }

    override suspend fun write(uri: String, bytes: ByteArray) {
        calls += "write $uri"
        writeFailure?.let { throw it }
        written = bytes.copyOf()
    }

    override suspend fun name(dbKey: String): String? = fileName

    override fun isShareAvailable(): Boolean = shareAvailable

    override suspend fun prepareShare(name: String, bytes: ByteArray): String {
        calls += "prepareShare $name"
        writeFailure?.let { throw it }
        written = bytes.copyOf()
        return shareUri
    }
}
