package ru.kino.dev.vault.sync

import ru.kino.dev.core.KeyFile
import ru.kino.dev.core.MergeSummary
import ru.kino.dev.core.MergedBothWays

/**
 * Another database file brought into the open one from the settings (pass-docs, Step 108): a copy of this database
 * from another device - merged - or a different database - imported. Two operations on two buttons; the core
 * checks the file is what the button says before anything changes.
 *
 * The file and the key file are read once, as picked - [read], apart from the merge: the read goes with its caller
 * when cancelled, the merge does not (pass-docs, Step 114). No access is kept, nothing is remembered. In memory
 * only - the caller saves.
 *
 * Throws [MergeFailure] and nothing else.
 */
interface OtherDatabaseRepository {

    /**
     * Reads the file at [uri] and the key file of [credentials] - for one merge; the caller wipes it after.
     *
     * Throws `FileUnreachable`, `KeyFileUnreadable`, `KeyFileTooLarge`.
     */
    suspend fun read(uri: String, credentials: OtherDatabaseCredentials): OtherDatabaseFile

    /** Merges [file] - a copy of the open database, opened with its own credentials - and reports what changed. */
    suspend fun mergeCopy(dbKey: String, file: OtherDatabaseFile): MergeSummary

    /**
     * Merges the copy [file] both ways (Step 110): into the open database, as [mergeCopy] does, and the bytes of the
     * copy's file back - encrypted with its own credentials, for [writeCopy] once the open database is written.
     */
    suspend fun mergeCopyBothWays(dbKey: String, file: OtherDatabaseFile): MergedBothWays

    /**
     * Writes [bytes] - from [mergeCopyBothWays] - over the copy's file at [uri].
     *
     * @throws MergeFailure.CopyNotWritten the file could not be written
     */
    suspend fun writeCopy(uri: String, bytes: ByteArray)

    /** Brings the different database [file], opened with its own credentials, into the open one, whole. */
    suspend fun importDatabase(dbKey: String, file: OtherDatabaseFile): MergeSummary

    /** The name of the file at [uri] to show, as the provider tells it; null when it does not. Never fails. */
    suspend fun name(uri: String): String?
}

/** What the other file is to the open database - the person says it with the button they press. */
enum class OtherDatabaseKind {
    /** A copy of this very database, from another device - merged. */
    Copy,

    /** A copy merged both ways: the result is written into this database and into the copy's file (Step 110). */
    CopyBothWays,

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

/**
 * Another database file as read for one merge ([OtherDatabaseRepository.read]): its bytes and what opens it. The key
 * file is open bytes - [wipe] once the merge is over, done or not.
 */
class OtherDatabaseFile(
    val bytes: ByteArray,
    val password: String?,
    val keyFile: KeyFile?,
) {
    fun wipe() {
        keyFile?.wipe()
    }

    // The password stays out of logs and crash reports
    override fun toString(): String = "OtherDatabaseFile(${bytes.size} bytes, keyFile=${keyFile != null})"
}
