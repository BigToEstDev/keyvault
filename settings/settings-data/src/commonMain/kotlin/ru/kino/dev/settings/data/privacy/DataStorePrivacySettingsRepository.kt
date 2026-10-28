package ru.kino.dev.settings.data.privacy

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import ru.kino.dev.settings.privacy.PrivacySettingsRepository
import java.io.IOException

/**
 * The privacy settings on the store of the app settings - their own keys in the shared file. A file that cannot
 * be read reads as the defaults - the screen stays closed; saving throws the [IOException] of the store.
 */
internal class DataStorePrivacySettingsRepository(
    private val store: DataStore<Preferences>,
) : PrivacySettingsRepository {

    private val preferences: Flow<Preferences> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }

    override val allowScreenshots: Flow<Boolean> = preferences
        .map { it[ALLOW_SCREENSHOTS_KEY] ?: false }
        .distinctUntilChanged()

    override suspend fun saveAllowScreenshots(on: Boolean) {
        store.edit { it[ALLOW_SCREENSHOTS_KEY] = on }
    }

    internal companion object {
        val ALLOW_SCREENSHOTS_KEY: Preferences.Key<Boolean> = booleanPreferencesKey("allow_screenshots")
    }
}
