package ru.kino.dev.vault

import ru.kino.dev.core.CoreException
import ru.kino.dev.core.MergeSummary

/** The open database: what was asked of it, and a failure to throw instead. */
class FakeVaultDatabase : VaultDatabaseRepository {
    val calls = mutableListOf<String>()
    var saveFailure: Exception? = null
    var lockFailure: CoreException? = null

    override suspend fun save(dbKey: String) {
        calls += "save"
        saveFailure?.let { throw it }
    }

    override suspend fun lock(dbKey: String) {
        calls += "lock"
        lockFailure?.let { throw it }
    }

    override suspend fun close(dbKey: String) = error("not here")
    override suspend fun hasChangedElsewhere(dbKey: String): Boolean = error("not here")
    override suspend fun merge(dbKey: String): MergeSummary = error("not here")
}
