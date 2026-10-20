package ru.kino.dev.settings.signin

import kotlinx.coroutines.flow.Flow

/** How one gets into a database - kept on the device, one for the app, not one per database (pass-docs, Step 59). */
interface SignInSettingsRepository {

    /** Whether a wrong password is wiped from its field; off until chosen. */
    val clearPasswordOnError: Flow<Boolean>

    /** Keeps whether a wrong password is wiped; throws an `IOException` when it could not be written. */
    suspend fun saveClearPasswordOnError(on: Boolean)
}
