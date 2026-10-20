package ru.kino.dev.settings.data.signin

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DataStoreSignInSettingsRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

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

    private fun newFile(): File = File(folder.newFolder(), "app_settings.preferences_pb")

    private fun TestScope.store(file: File): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(scope = backgroundScope) { file }
}
