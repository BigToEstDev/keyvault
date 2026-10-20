package ru.kino.dev.settings.data.lock

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import ru.kino.dev.settings.lock.AutoLockTimeout
import ru.kino.dev.settings.lock.LockSettingsRepository
import java.io.IOException

/**
 * The lock settings on the store of the app settings - their own keys in the shared file.
 *
 * Kept as codes of their own rather than the names of the enum, as the appearance is. A code this version
 * does not know reads as the default, and so does a file that cannot be read: the database still locks, at
 * the time of Step 46. Saving throws the [IOException] of the store.
 */
internal class DataStoreLockSettingsRepository(
    private val store: DataStore<Preferences>,
) : LockSettingsRepository {

    override val autoLockTimeout: Flow<AutoLockTimeout> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { preferences ->
            AUTO_LOCK_TIMEOUT.entries.firstOrNull { it.value == preferences[AUTO_LOCK_TIMEOUT_KEY] }?.key
                ?: AutoLockTimeout.Default
        }
        .distinctUntilChanged()

    override suspend fun save(timeout: AutoLockTimeout) {
        store.edit { it[AUTO_LOCK_TIMEOUT_KEY] = AUTO_LOCK_TIMEOUT.getValue(timeout) }
    }

    internal companion object {
        val AUTO_LOCK_TIMEOUT_KEY: Preferences.Key<String> = stringPreferencesKey("auto_lock_timeout")

        val AUTO_LOCK_TIMEOUT = mapOf(
            AutoLockTimeout.Seconds30 to "30s",
            AutoLockTimeout.Minute1 to "1m",
            AutoLockTimeout.Minutes2 to "2m",
            AutoLockTimeout.Minutes5 to "5m",
            AutoLockTimeout.Minutes10 to "10m",
            AutoLockTimeout.Minutes15 to "15m",
            AutoLockTimeout.Minutes30 to "30m",
        )
    }
}
