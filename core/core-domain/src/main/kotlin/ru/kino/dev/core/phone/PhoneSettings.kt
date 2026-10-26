package ru.kino.dev.core.phone

import kotlinx.coroutines.flow.Flow

/**
 * The prefix a new phone field starts with (pass-docs, Step 77, decisions 6 and 9): "remember the prefix" on - the
 * country chosen in the settings; off, or none chosen - the country of the device. Of the app, not of a database.
 */
interface PhoneSettingsRepository {

    /** Whether the prefix chosen is remembered; off until switched. */
    val rememberPrefix: Flow<Boolean>

    /** The country chosen - its code; null until one is. */
    val prefixRegion: Flow<String?>

    /** Keeps whether the prefix is remembered; throws an `IOException` when it could not be written. */
    suspend fun saveRememberPrefix(on: Boolean)

    /** Keeps the country chosen; throws an `IOException` when it could not be written. */
    suspend fun savePrefixRegion(region: String)
}

/** The country a new phone field starts with. */
fun interface PhoneDefaults {

    /** Its code: as [PhoneSettingsRepository] says, else of the device, else of the first calling code. */
    suspend fun region(): String
}
