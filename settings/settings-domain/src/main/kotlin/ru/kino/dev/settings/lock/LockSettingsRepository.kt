package ru.kino.dev.settings.lock

import kotlinx.coroutines.flow.Flow

/** How the open database locks - kept on the device, one for the app, not one per database. */
interface LockSettingsRepository {

    /** The chosen time without a touch, [AutoLockTimeout.Default] until one is chosen; a new value on every change. */
    val autoLockTimeout: Flow<AutoLockTimeout>

    /** Whether the app going off the screen locks at once, not waiting for the time; off until chosen. */
    val lockOnLeave: Flow<Boolean>

    /** Whether the screens of the open database have the button that locks it now; off until chosen. */
    val lockButton: Flow<Boolean>

    /** Keeps [timeout]; throws an `IOException` when it could not be written. */
    suspend fun save(timeout: AutoLockTimeout)

    /** Keeps whether leaving locks; throws an `IOException` when it could not be written. */
    suspend fun saveLockOnLeave(on: Boolean)

    /** Keeps whether the lock button is shown; throws an `IOException` when it could not be written. */
    suspend fun saveLockButton(on: Boolean)
}
