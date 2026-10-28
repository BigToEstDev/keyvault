package ru.kino.dev.settings.data.presets

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

class DataStorePresetSettingsRepositoryTest {

    private val folder = TemporaryFolder()

    @AfterTest
    fun deleteFolder() = folder.delete()

    @Test
    fun `presets are used until chosen otherwise`() = runTest {
        val repository = DataStorePresetSettingsRepository(store(newFile()))

        assertEquals(true, repository.usePresets.first())
    }

    @Test
    fun `presets switched off is what is read next`() = runTest {
        val repository = DataStorePresetSettingsRepository(store(newFile()))

        repository.saveUsePresets(false)

        assertEquals(false, repository.usePresets.first())
    }

    private fun newFile(): File = File(folder.newFolder(), "app_settings.preferences_pb")

    private fun TestScope.store(file: File): DataStore<Preferences> =
        PreferenceDataStoreFactory.createWithPath(scope = backgroundScope) { file.absolutePath.toPath() }
}
