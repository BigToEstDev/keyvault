package ru.kino.dev.settings.presets

import kotlinx.coroutines.flow.Flow

/** Whether the app works with presets - kept on the device, one for the app (pass-docs, Step 62). */
interface PresetSettingsRepository {

    /**
     * Whether the screens of an entry have the choice of presets; on until chosen. Off - a new entry takes the
     * standard preset, one saved keeps its own.
     */
    val usePresets: Flow<Boolean>

    /** Keeps whether presets are used; throws an `IOException` when it could not be written. */
    suspend fun saveUsePresets(on: Boolean)
}
