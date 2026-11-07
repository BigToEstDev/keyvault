package ru.kino.dev.vault.sync

import ru.kino.dev.core.MergeSummary

/**
 * Another database file brought into the open one from the settings (pass-docs, Step 108): a copy of this database
 * from another device - merged - or a different database - imported. Two operations on two buttons; the core
 * checks the file is what the button says before anything changes.
 *
 * The file and the key file are read once, as picked: no access is kept, nothing is remembered. In memory only -
 * the caller saves.
 *
 * Throws [MergeFailure] and nothing else.
 */
interface OtherDatabaseRepository {

    /** Merges the copy of the open database at [uri], opened with its own [credentials], and reports what changed. */
    suspend fun mergeCopy(dbKey: String, uri: String, credentials: OtherDatabaseCredentials): MergeSummary

    /** Brings the different database at [uri], opened with its own [credentials], into the open one, whole. */
    suspend fun importDatabase(dbKey: String, uri: String, credentials: OtherDatabaseCredentials): MergeSummary

    /** The name of the file at [uri] to show, as the provider tells it; null when it does not. Never fails. */
    suspend fun name(uri: String): String?
}

/** What the other file is to the open database - the person says it with the button they press. */
enum class OtherDatabaseKind {
    /** A copy of this very database, from another device - merged. */
    Copy,

    /** A different database - brought in whole. */
    Import,
}

/**
 * What opens the other database - its own, may differ from the open one's. At least one of the two.
 *
 * @property keyFileUri the key file as picked; read once, not kept
 */
data class OtherDatabaseCredentials(
    val password: String?,
    val keyFileUri: String?,
) {
    init {
        require(password != null || keyFileUri != null) { "a database opens with a password or a key file" }
    }

    // The password stays out of logs and crash reports
    override fun toString(): String =
        "OtherDatabaseCredentials(password=${if (password == null) "none" else "***"}, keyFile=${keyFileUri != null})"
}
