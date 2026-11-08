package ru.kino.dev.vault.sync

import ru.kino.dev.core.MergeSummary
import ru.kino.dev.core.MergedBothWays

/** Another database file: what was asked of it, and a failure to throw instead. */
class FakeOtherDatabaseRepository : OtherDatabaseRepository {

    /** `"<mergeCopy|mergeCopyBothWays|importDatabase|writeCopy> <uri>"` for every merge and write asked. */
    val calls = mutableListOf<String>()

    /** The credentials of the last merge. */
    var credentialsReceived: OtherDatabaseCredentials? = null

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

    override suspend fun mergeCopy(dbKey: String, uri: String, credentials: OtherDatabaseCredentials): MergeSummary =
        merge("mergeCopy", uri, credentials)

    /** The bytes of the copy's file [mergeCopyBothWays] hands back. */
    var copyBytes = byteArrayOf(7, 7, 7)

    /** The bytes the last [writeCopy] wrote. */
    var copyWritten: ByteArray? = null

    /** Thrown by [writeCopy] instead of writing. */
    var writeFailure: MergeFailure? = null

    override suspend fun mergeCopyBothWays(
        dbKey: String,
        uri: String,
        credentials: OtherDatabaseCredentials,
    ): MergedBothWays = MergedBothWays(merge("mergeCopyBothWays", uri, credentials), copyBytes.copyOf())

    override suspend fun writeCopy(uri: String, bytes: ByteArray) {
        calls += "writeCopy $uri"
        writeFailure?.let { throw it }
        copyWritten = bytes.copyOf()
    }

    override suspend fun importDatabase(dbKey: String, uri: String, credentials: OtherDatabaseCredentials): MergeSummary =
        merge("importDatabase", uri, credentials)

    override suspend fun name(uri: String): String? = names[uri]

    private fun merge(method: String, uri: String, credentials: OtherDatabaseCredentials): MergeSummary {
        calls += "$method $uri"
        credentialsReceived = credentials
        failure?.let { throw it }
        return summary
    }
}
