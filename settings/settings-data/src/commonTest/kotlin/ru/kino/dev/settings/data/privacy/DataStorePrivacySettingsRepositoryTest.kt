package ru.kino.dev.settings.data.privacy

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

class DataStorePrivacySettingsRepositoryTest {

    private val folder = TemporaryFolder()

    @AfterTest
    fun deleteFolder() = folder.delete()

    @Test
    fun `screenshots are not allowed until chosen`() = runTest {
        val repository = DataStorePrivacySettingsRepository(store(newFile()))

        assertEquals(false, repository.allowScreenshots.first())
    }

    @Test
    fun `screenshots allowed is what is read next`() = runTest {
        val repository = DataStorePrivacySettingsRepository(store(newFile()))

        repository.saveAllowScreenshots(true)

        assertEquals(true, repository.allowScreenshots.first())
    }

    private fun newFile(): File = File(folder.newFolder(), "app_settings.preferences_pb")

    private fun TestScope.store(file: File): DataStore<Preferences> =
        PreferenceDataStoreFactory.createWithPath(scope = backgroundScope) { file.absolutePath.toPath() }
}
