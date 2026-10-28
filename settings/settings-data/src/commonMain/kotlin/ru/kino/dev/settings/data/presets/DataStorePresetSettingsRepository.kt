package ru.kino.dev.settings.data.presets

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import ru.kino.dev.settings.presets.PresetSettingsRepository
import java.io.IOException

/**
 * The preset settings on the store of the app settings - their own keys in the shared file. A file that cannot be
 * read reads as the defaults; saving throws the [IOException] of the store. The presets themselves are not here:
 * they are `core`'s (pass-docs, Step 44).
 */
internal class DataStorePresetSettingsRepository(
    private val store: DataStore<Preferences>,
) : PresetSettingsRepository {

    private val preferences: Flow<Preferences> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }

    override val usePresets: Flow<Boolean> = preferences
        .map { it[USE_PRESETS_KEY] ?: true }
        .distinctUntilChanged()

    override suspend fun saveUsePresets(on: Boolean) {
        store.edit { it[USE_PRESETS_KEY] = on }
    }

    internal companion object {
        val USE_PRESETS_KEY: Preferences.Key<Boolean> = booleanPreferencesKey("use_presets")
    }
}
