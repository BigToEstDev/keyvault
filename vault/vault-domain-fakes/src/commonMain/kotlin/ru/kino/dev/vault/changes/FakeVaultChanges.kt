package ru.kino.dev.vault.changes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** [VaultChanges] moved by hand: a test says the database changed elsewhere with [changed]. */
class FakeVaultChanges : VaultChanges {

    private val versions = MutableStateFlow<Map<String, Long>>(emptyMap())

    override fun versions(dbKey: String): Flow<Long> = versions.map { it[dbKey] ?: 0L }.distinctUntilChanged()

    override fun current(dbKey: String): Long = versions.value[dbKey] ?: 0L

    fun changed(dbKey: String) {
        versions.update { it + (dbKey to (it[dbKey] ?: 0L) + 1) }
    }
}
