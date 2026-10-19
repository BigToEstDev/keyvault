package ru.kino.dev.settings.data.appearance

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import ru.kino.dev.settings.appearance.AppearanceRepository
import ru.kino.dev.settings.appearance.ThemeMode
import java.io.IOException

/**
 * The appearance on the store of the app settings - its own keys in the shared file.
 *
 * The mode is kept as a code of its own rather than the name of the enum, so renaming a value in the code
 * does not lose what was chosen. A code this version does not know reads as [ThemeMode.System], and so does
 * a file that cannot be read: the theme is not worth an app that fails to start. Saving throws the
 * [IOException] of the store.
 */
internal class DataStoreAppearanceRepository(
    private val store: DataStore<Preferences>,
) : AppearanceRepository {

    override val themeMode: Flow<ThemeMode> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { preferences ->
            THEME_MODE.entries.firstOrNull { it.value == preferences[THEME_MODE_KEY] }?.key ?: ThemeMode.System
        }
        .distinctUntilChanged()

    override suspend fun save(mode: ThemeMode) {
        store.edit { it[THEME_MODE_KEY] = THEME_MODE.getValue(mode) }
    }

    internal companion object {
        val THEME_MODE_KEY: Preferences.Key<String> = stringPreferencesKey("theme_mode")

        val THEME_MODE = mapOf(
            ThemeMode.System to "system",
            ThemeMode.Light to "light",
            ThemeMode.Dark to "dark",
        )
    }
}
