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

    /** What [hasChangedElsewhere] answers. */
    var changedElsewhere = false

    /** Thrown by [hasChangedElsewhere] instead of the answer - the file cannot be read now, say. */
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
        calls += "save"
        saveFailure?.let { throw it }
        onSave()
    }

    override suspend fun lock(dbKey: String) {
        calls += "lock"
        lockFailure?.let { throw it }
    }

    override suspend fun close(dbKey: String) = error("not here")

    override suspend fun hasChangedElsewhere(dbKey: String): Boolean {
        calls += "hasChangedElsewhere"
        checkFailure?.let { throw it }
        return changedElsewhere
    }

    override suspend fun merge(dbKey: String): MergeSummary {
        calls += "merge"
        mergeFailure?.let { throw it }
        changedElsewhere = false
        return mergeSummary
    }
}
