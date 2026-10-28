package ru.kino.dev.phone

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import ru.kino.dev.core.phone.PhoneSettingsRepository
import java.io.IOException

/**
 * [PhoneSettingsRepository] on a preferences data store of its own - of the app, not of a database. What cannot be
 * read reads as nothing chosen: the prefix of the device.
 */
internal class DataStorePhoneSettingsRepository(
    private val store: DataStore<Preferences>,
) : PhoneSettingsRepository {

    private val data: Flow<Preferences> = store.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }

    override val rememberPrefix: Flow<Boolean> = data.map { it[REMEMBER_KEY] ?: false }.distinctUntilChanged()

    override val prefixRegion: Flow<String?> = data.map { it[REGION_KEY] }.distinctUntilChanged()

    override suspend fun saveRememberPrefix(on: Boolean) {
        store.edit { it[REMEMBER_KEY] = on }
    }

    override suspend fun savePrefixRegion(region: String) {
        store.edit { it[REGION_KEY] = region }
    }

    internal companion object {
        val REMEMBER_KEY: Preferences.Key<Boolean> = booleanPreferencesKey("remember_prefix")
        val REGION_KEY: Preferences.Key<String> = stringPreferencesKey("prefix_region")
    }
}
