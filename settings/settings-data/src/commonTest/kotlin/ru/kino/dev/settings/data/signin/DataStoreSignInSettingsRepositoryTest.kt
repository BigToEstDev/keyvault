package ru.kino.dev.settings.data.signin

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import ru.kino.dev.core.TemporaryFolder

class DataStoreSignInSettingsRepositoryTest {

    private val folder = TemporaryFolder()

    @AfterTest
    fun deleteFolder() = folder.delete()

    @Test
    fun `a wrong password is not wiped until chosen`() = runTest {
        val repository = DataStoreSignInSettingsRepository(store(newFile()))

        assertEquals(false, repository.clearPasswordOnError.first())
    }

    @Test
    fun `wiping switched on is what is read next`() = runTest {
        val repository = DataStoreSignInSettingsRepository(store(newFile()))

        repository.saveClearPasswordOnError(true)

        assertEquals(true, repository.clearPasswordOnError.first())
    }

    @Test
    fun `biometrics are not asked by themselves until chosen`() = runTest {
        val repository = DataStoreSignInSettingsRepository(store(newFile()))

        assertEquals(false, repository.autoBiometricPrompt.first())
    }

    @Test
    fun `the auto prompt switched on is what is read next`() = runTest {
        val repository = DataStoreSignInSettingsRepository(store(newFile()))

        repository.saveAutoBiometricPrompt(true)

        assertEquals(true, repository.autoBiometricPrompt.first())
    }

    private fun newFile(): File = File(folder.newFolder(), "app_settings.preferences_pb")

    private fun TestScope.store(file: File): DataStore<Preferences> =
        PreferenceDataStoreFactory.createWithPath(scope = backgroundScope) { file.absolutePath.toPath() }
}
