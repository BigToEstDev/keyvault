package ru.kino.dev.vault.sync

import ru.kino.dev.core.KeyFile
import ru.kino.dev.core.MergeSummary
import ru.kino.dev.core.MergedBothWays

/** Another database file: what was asked of it, and a failure to throw instead. */
class FakeOtherDatabaseRepository : OtherDatabaseRepository {

    /** `"<read|mergeCopy|mergeCopyBothWays|importDatabase|writeCopy> <uri>"` for every read, merge and write asked. */
    val calls = mutableListOf<String>()

    /** The credentials of the last merge. */
    var credentialsReceived: OtherDatabaseCredentials? = null

    /** Run first by [read] - a test holds the read of the file here. */
    var beforeRead: suspend () -> Unit = {}

    /** Thrown by [read] instead of the file. */
    var readFailure: MergeFailure? = null

    /** The files [read] handed out, to see they were wiped. */
    val filesRead = mutableListOf<OtherDatabaseFile>()

    // The uri each file was read from, for the calls
    private val uris = mutableMapOf<OtherDatabaseFile, String>()

    /** Thrown instead of [summary]. */
    var failure: MergeFailure? = null

    /** The names the provider tells, by uri. */
    val names = mutableMapOf<String, String>()

    var summary = MergeSummary(
        addedGroups = 1,
        updatedGroups = 0,
        movedGroups = 0,
        addedEntries = 2,
        updatedEntries = 1,
        movedEntries = 0,
        deletedGroups = 0,
        deletedEntries = 1,
        metaDataChanged = false,
        mergeDone = true,
        differentDatabases = false,
    )

    override suspend fun read(uri: String, credentials: OtherDatabaseCredentials): OtherDatabaseFile {
        beforeRead()
        calls += "read $uri"
        readFailure?.let { throw it }
        credentialsReceived = credentials
        val keyFile = credentials.keyFileUri?.let { KeyFile(it, byteArrayOf(1, 2, 3)) }
        val file = OtherDatabaseFile(byteArrayOf(4, 2), credentials.password, keyFile)
        filesRead += file
        uris[file] = uri
        return file
    }

    override suspend fun mergeCopy(dbKey: String, file: OtherDatabaseFile): MergeSummary = merge("mergeCopy", file)

    /** The bytes of the copy's file [mergeCopyBothWays] hands back. */
    var copyBytes = byteArrayOf(7, 7, 7)

    /** The bytes the last [writeCopy] wrote. */
    var copyWritten: ByteArray? = null

    /** Thrown by [writeCopy] instead of writing. */
    var writeFailure: MergeFailure? = null

    override suspend fun mergeCopyBothWays(dbKey: String, file: OtherDatabaseFile): MergedBothWays =
        MergedBothWays(merge("mergeCopyBothWays", file), copyBytes.copyOf())

    override suspend fun writeCopy(uri: String, bytes: ByteArray) {
        calls += "writeCopy $uri"
        writeFailure?.let { throw it }
        copyWritten = bytes.copyOf()
    }

    override suspend fun importDatabase(dbKey: String, file: OtherDatabaseFile): MergeSummary =
        merge("importDatabase", file)

    override suspend fun name(uri: String): String? = names[uri]

    private fun merge(method: String, file: OtherDatabaseFile): MergeSummary {
        calls += "$method ${uris.getValue(file)}"
        failure?.let { throw it }
        return summary
    }
}
