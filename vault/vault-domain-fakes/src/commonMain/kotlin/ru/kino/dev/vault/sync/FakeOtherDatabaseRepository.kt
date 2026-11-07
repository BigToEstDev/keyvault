package ru.kino.dev.vault.sync

import ru.kino.dev.core.MergeSummary

/** Another database file: what was asked of it, and a failure to throw instead. */
class FakeOtherDatabaseRepository : OtherDatabaseRepository {

    /** `"<mergeCopy|importDatabase> <uri>"` for every merge asked. */
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
