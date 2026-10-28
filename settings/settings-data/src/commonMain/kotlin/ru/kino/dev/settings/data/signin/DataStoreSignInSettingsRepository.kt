package ru.kino.dev.settings.data.signin

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import ru.kino.dev.settings.signin.SignInSettingsRepository
import java.io.IOException

/**
 * The sign-in settings on the store of the app settings - their own keys in the shared file. A file that cannot
 * be read reads as the defaults; saving throws the [IOException] of the store.
 */
internal class DataStoreSignInSettingsRepository(
    private val store: DataStore<Preferences>,
) : SignInSettingsRepository {

    private val preferences: Flow<Preferences> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }

    override val clearPasswordOnError: Flow<Boolean> = preferences
        .map { it[CLEAR_PASSWORD_ON_ERROR_KEY] ?: false }
        .distinctUntilChanged()

    override val autoBiometricPrompt: Flow<Boolean> = preferences
        .map { it[AUTO_BIOMETRIC_PROMPT_KEY] ?: false }
        .distinctUntilChanged()

    override suspend fun saveClearPasswordOnError(on: Boolean) {
        store.edit { it[CLEAR_PASSWORD_ON_ERROR_KEY] = on }
    }

    override suspend fun saveAutoBiometricPrompt(on: Boolean) {
        store.edit { it[AUTO_BIOMETRIC_PROMPT_KEY] = on }
    }

    internal companion object {
        val CLEAR_PASSWORD_ON_ERROR_KEY: Preferences.Key<Boolean> = booleanPreferencesKey("clear_password_on_error")
        val AUTO_BIOMETRIC_PROMPT_KEY: Preferences.Key<Boolean> = booleanPreferencesKey("auto_biometric_prompt")
    }
}
