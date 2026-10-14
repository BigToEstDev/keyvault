package ru.kino.dev.vault.data.filter

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import ru.kino.dev.vault.filter.CreationOrder
import ru.kino.dev.vault.filter.VaultFilter
import ru.kino.dev.vault.filter.VaultFilterRepository
import ru.kino.dev.vault.filter.Visibility

/**
 * The filter of the list on a preferences data store - one for the app, not one per database.
 *
 * Each choice is kept as a code of its own rather than the name of the enum, so renaming a value in the
 * code does not lose what was chosen. A code this version does not know reads as the default: a filter
 * that cannot be read is not worth a list that fails to show.
 */
internal class DataStoreVaultFilterRepository(
    private val store: DataStore<Preferences>,
) : VaultFilterRepository {

    override val filter: Flow<VaultFilter> = store.data
        .map { preferences ->
            VaultFilter(
                visibility = VISIBILITY.entries.firstOrNull { it.value == preferences[VISIBILITY_KEY] }?.key
                    ?: VaultFilter.DEFAULT.visibility,
                order = ORDER.entries.firstOrNull { it.value == preferences[ORDER_KEY] }?.key
                    ?: VaultFilter.DEFAULT.order,
            )
        }
        .distinctUntilChanged()

    override suspend fun save(filter: VaultFilter) {
        store.edit { preferences ->
            preferences[VISIBILITY_KEY] = VISIBILITY.getValue(filter.visibility)
            preferences[ORDER_KEY] = ORDER.getValue(filter.order)
        }
    }

    internal companion object {
        val VISIBILITY_KEY: Preferences.Key<String> = stringPreferencesKey("visibility")
        val ORDER_KEY: Preferences.Key<String> = stringPreferencesKey("order")

        val VISIBILITY = mapOf(
            Visibility.All to "all",
            Visibility.GroupsOnly to "groups",
            Visibility.EntriesOnly to "entries",
        )
        val ORDER = mapOf(
            CreationOrder.NewestFirst to "newest",
            CreationOrder.OldestFirst to "oldest",
        )
    }
}

// The extension is the documented way to declare a store: one instance per file for the whole process
internal val Context.vaultFilterStore: DataStore<Preferences> by preferencesDataStore(name = "vault_filter")
