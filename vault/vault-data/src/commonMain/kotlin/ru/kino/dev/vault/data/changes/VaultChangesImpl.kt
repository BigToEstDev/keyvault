package ru.kino.dev.vault.data.changes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import ru.kino.dev.vault.changes.VaultChanges

/**
 * [VaultChanges] of the process: the repositories of `vault` call [changed] after each write that went through. One
 * for the app - the screens listen to the same versions the repositories move.
 */
internal class VaultChangesImpl : VaultChanges {

    private val versions = MutableStateFlow<Map<String, Long>>(emptyMap())

    override fun versions(dbKey: String): Flow<Long> = versions.map { it[dbKey] ?: 0L }.distinctUntilChanged()

    override fun current(dbKey: String): Long = versions.value[dbKey] ?: 0L

    /** The database [dbKey] has changed in memory. */
    fun changed(dbKey: String) {
        versions.update { it + (dbKey to (it[dbKey] ?: 0L) + 1) }
    }
}
