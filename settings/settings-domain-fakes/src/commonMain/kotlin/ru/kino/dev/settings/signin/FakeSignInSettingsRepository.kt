package ru.kino.dev.settings.signin

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.IOException

/** The sign-in settings in memory; [failSave] - nothing is written, as with a full disk. */
class FakeSignInSettingsRepository(
    clearPasswordOnError: Boolean = false,
    autoBiometricPrompt: Boolean = false,
    private val failSave: Boolean = false,
) : SignInSettingsRepository {

    private val clearing = MutableStateFlow(clearPasswordOnError)

    val savedClearPasswordOnError = mutableListOf<Boolean>()

    private val prompting = MutableStateFlow(autoBiometricPrompt)

    val savedAutoBiometricPrompt = mutableListOf<Boolean>()

    override val clearPasswordOnError: Flow<Boolean> = clearing

    override val autoBiometricPrompt: Flow<Boolean> = prompting

    override suspend fun saveClearPasswordOnError(on: Boolean) {
        if (failSave) throw IOException("disk full")
        savedClearPasswordOnError += on
        clearing.value = on
    }

    override suspend fun saveAutoBiometricPrompt(on: Boolean) {
        if (failSave) throw IOException("disk full")
        savedAutoBiometricPrompt += on
        prompting.value = on
    }
}
