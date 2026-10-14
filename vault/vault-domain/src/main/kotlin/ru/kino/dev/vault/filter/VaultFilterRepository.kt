package ru.kino.dev.vault.filter

import kotlinx.coroutines.flow.Flow

/**
 * The filter of the list, kept on the device - not in the database: it is how this app shows any database,
 * and nothing in it is secret.
 */
interface VaultFilterRepository {

    /** The filter chosen last, [VaultFilter.DEFAULT] until one is; a new value on every change. */
    val filter: Flow<VaultFilter>

    /** Throws an `IOException` when the device does not take it; the filter chosen is still in effect. */
    suspend fun save(filter: VaultFilter)
}
