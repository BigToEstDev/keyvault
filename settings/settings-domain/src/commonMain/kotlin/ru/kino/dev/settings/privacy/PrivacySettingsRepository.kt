package ru.kino.dev.settings.privacy

import kotlinx.coroutines.flow.Flow

/** What the app lets out of its screen - kept on the device, one for the app (pass-docs, Step 61). */
interface PrivacySettingsRepository {

    /**
     * Whether the screen of the app may be captured - screenshots, recording, casting, the preview in the recent
     * apps; off until chosen, in debug as in release.
     */
    val allowScreenshots: Flow<Boolean>

    /** Keeps whether screenshots are allowed; throws an `IOException` when it could not be written. */
    suspend fun saveAllowScreenshots(on: Boolean)
}
