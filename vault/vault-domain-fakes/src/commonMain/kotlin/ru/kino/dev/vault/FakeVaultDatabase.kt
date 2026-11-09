package ru.kino.dev.vault

import ru.kino.dev.core.CoreException
import ru.kino.dev.core.MergeSummary

/** The open database: what was asked of it, and a failure to throw instead. */
class FakeVaultDatabase : VaultDatabaseRepository {
    val calls = mutableListOf<String>()
    var saveFailure: Exception? = null
    var lockFailure: CoreException? = null

    /** Run by a [save] that did not throw - what the real one leaves in `SaveStatus`, say. */
    var onSave: () -> Unit = {}

    /** Run first by [save] - a test holds the write here to cancel the change between its steps. */
    var beforeSave: suspend () -> Unit = {}

    /** Run first by [read] - a test holds the read of the file here. */
    var beforeRead: suspend () -> Unit = {}

    /** Thrown by [read] instead of the bytes - the file cannot be read now, say. */
    var readFailure: Exception? = null

    /** What [read] answers: the file as it is now. */
    var fileBytes = byteArrayOf(1, 2, 3)

    /** The bytes the last [merge] was given. */
    var mergedBytes: ByteArray? = null

    /** What [hasChangedElsewhere] answers. */
    var changedElsewhere = false

    /** Thrown by [hasChangedElsewhere] instead of the answer - the bytes are not a database, say. */
    var checkFailure: Exception? = null

    /** Thrown by [merge] instead of [mergeSummary]. */
    var mergeFailure: Exception? = null

    /** What [merge] reports. */
    var mergeSummary = MergeSummary(
        addedGroups = 0,
        updatedGroups = 0,
        movedGroups = 0,
        addedEntries = 1,
        updatedEntries = 0,
        movedEntries = 0,
        deletedGroups = 0,
        deletedEntries = 0,
        metaDataChanged = false,
        mergeDone = true,
        differentDatabases = false,
    )

    override suspend fun save(dbKey: String) {
        beforeSave()
        calls += "save"
        saveFailure?.let { throw it }
        onSave()
    }

    override suspend fun lock(dbKey: String) {
        calls += "lock"
        lockFailure?.let { throw it }
    }

    override suspend fun close(dbKey: String) = error("not here")

    override suspend fun read(dbKey: String): ByteArray {
        beforeRead()
        calls += "read"
        readFailure?.let { throw it }
        return fileBytes.copyOf()
    }

    override suspend fun hasChangedElsewhere(dbKey: String, bytes: ByteArray): Boolean {
        calls += "hasChangedElsewhere"
        checkFailure?.let { throw it }
        return changedElsewhere
    }

    override suspend fun merge(dbKey: String, bytes: ByteArray): MergeSummary {
        calls += "merge"
        mergedBytes = bytes.copyOf()
        mergeFailure?.let { throw it }
        changedElsewhere = false
        return mergeSummary
    }
}
