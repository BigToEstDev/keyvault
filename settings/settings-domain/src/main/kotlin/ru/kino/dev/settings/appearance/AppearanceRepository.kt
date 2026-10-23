package ru.kino.dev.settings.appearance

import kotlinx.coroutines.flow.Flow

/** How the app looks - kept on the device, one for the app, not one per database. */
interface AppearanceRepository {

    /** The chosen mode, [ThemeMode.System] until one is chosen; a new value on every change. */
    val themeMode: Flow<ThemeMode>

    /** The chosen language, [AppLanguage.System] until one is chosen; a new value on every change. */
    val language: Flow<AppLanguage>

    /** Whether a secret shown in clear is colored by its letters, digits and symbols; on until chosen. */
    val passwordColoring: Flow<Boolean>

    /** Whether a password being made shows its strength on its field; off until chosen (pass-docs, Step 74). */
    val passwordStrength: Flow<Boolean>

    /** Whether the card of an entry in the list shows its user name; off until chosen. */
    val userNameInList: Flow<Boolean>

    /** Whether the icon of a group has the number of its entries; on until chosen. */
    val entryCount: Flow<Boolean>

    /** Whether that number counts the groups below too; on until chosen. */
    val entryCountNested: Flow<Boolean>

    /** Keeps [mode]; throws an `IOException` when it could not be written. */
    suspend fun save(mode: ThemeMode)

    /** Keeps [language]; throws an `IOException` when it could not be written. */
    suspend fun save(language: AppLanguage)

    /** Keeps whether passwords are colored; throws an `IOException` when it could not be written. */
    suspend fun savePasswordColoring(on: Boolean)

    /** Keeps whether the strength of a password is shown; throws an `IOException` when it could not be written. */
    suspend fun savePasswordStrength(on: Boolean)

    /** Keeps whether the list shows user names; throws an `IOException` when it could not be written. */
    suspend fun saveUserNameInList(on: Boolean)

    /** Keeps whether groups show their number; throws an `IOException` when it could not be written. */
    suspend fun saveEntryCount(on: Boolean)

    /** Keeps whether the number counts the groups below; throws an `IOException` when it could not be written. */
    suspend fun saveEntryCountNested(on: Boolean)
}
