package ru.kino.dev.settings.data.appearance

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
import ru.kino.dev.settings.appearance.AppLanguage
import ru.kino.dev.settings.appearance.AppearanceRepository
import ru.kino.dev.settings.appearance.ThemeMode
import java.io.IOException

/**
 * The appearance on the store of the app settings - its own keys in the shared file.
 *
 * The mode and the language are kept as codes of their own rather than the names of the enums, so renaming
 * a value in the code does not lose what was chosen. A code this version does not know reads as the system
 * one, and so does a file that cannot be read: the look is not worth an app that fails to start. Saving
 * throws the [IOException] of the store.
 */
internal class DataStoreAppearanceRepository(
    private val store: DataStore<Preferences>,
) : AppearanceRepository {

    private val preferences: Flow<Preferences> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }

    override val themeMode: Flow<ThemeMode> = preferences
        .map { THEME_MODE.read(it[THEME_MODE_KEY]) ?: ThemeMode.System }
        .distinctUntilChanged()

    override val language: Flow<AppLanguage> = preferences
        .map { LANGUAGE.read(it[LANGUAGE_KEY]) ?: AppLanguage.System }
        .distinctUntilChanged()

    override val passwordColoring: Flow<Boolean> = preferences
        .map { it[PASSWORD_COLORING_KEY] ?: true }
        .distinctUntilChanged()

    override suspend fun save(mode: ThemeMode) {
        store.edit { it[THEME_MODE_KEY] = THEME_MODE.getValue(mode) }
    }

    override suspend fun save(language: AppLanguage) {
        store.edit { it[LANGUAGE_KEY] = LANGUAGE.getValue(language) }
    }

    override suspend fun savePasswordColoring(on: Boolean) {
        store.edit { it[PASSWORD_COLORING_KEY] = on }
    }

    internal companion object {
        val THEME_MODE_KEY: Preferences.Key<String> = stringPreferencesKey("theme_mode")
        val LANGUAGE_KEY: Preferences.Key<String> = stringPreferencesKey("language")
        val PASSWORD_COLORING_KEY: Preferences.Key<Boolean> = booleanPreferencesKey("password_coloring")

        val THEME_MODE = mapOf(
            ThemeMode.System to "system",
            ThemeMode.Light to "light",
            ThemeMode.Dark to "dark",
        )

        val LANGUAGE = mapOf(
            AppLanguage.System to "system",
            AppLanguage.English to "en",
            AppLanguage.Russian to "ru",
        )

        private fun <T> Map<T, String>.read(code: String?): T? = entries.firstOrNull { it.value == code }?.key
    }
}
