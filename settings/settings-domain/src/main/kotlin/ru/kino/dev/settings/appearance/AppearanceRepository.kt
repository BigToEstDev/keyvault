package ru.kino.dev.settings.appearance

import kotlinx.coroutines.flow.Flow

/** How the app looks - kept on the device, one for the app, not one per database. */
interface AppearanceRepository {

    /** The chosen mode, [ThemeMode.System] until one is chosen; a new value on every change. */
    val themeMode: Flow<ThemeMode>

    /** The chosen language, [AppLanguage.System] until one is chosen; a new value on every change. */
    val language: Flow<AppLanguage>

    /** Keeps [mode]; throws an `IOException` when it could not be written. */
    suspend fun save(mode: ThemeMode)

    /** Keeps [language]; throws an `IOException` when it could not be written. */
    suspend fun save(language: AppLanguage)
}
