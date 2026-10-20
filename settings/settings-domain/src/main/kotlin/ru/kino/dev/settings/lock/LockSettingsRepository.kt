package ru.kino.dev.settings.lock

import kotlinx.coroutines.flow.Flow

/** How the open database locks - kept on the device, one for the app, not one per database. */
interface LockSettingsRepository {

    /** The chosen time without a touch, [AutoLockTimeout.Default] until one is chosen; a new value on every change. */
    val autoLockTimeout: Flow<AutoLockTimeout>

    /** Keeps [timeout]; throws an `IOException` when it could not be written. */
    suspend fun save(timeout: AutoLockTimeout)
}
