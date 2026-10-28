package ru.kino.dev.settings.data.lock

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import ru.kino.dev.settings.lock.AutoLockTimeout
import java.io.File

class DataStoreLockSettingsRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun `nothing chosen yet reads as five minutes`() = runTest {
        val repository = DataStoreLockSettingsRepository(store(newFile()))

        assertEquals(AutoLockTimeout.Minutes5, repository.autoLockTimeout.first())
    }

    @Test
    fun `a saved time is what is read next, kept as a code of its own`() = runTest {
        val store = store(newFile())
        val repository = DataStoreLockSettingsRepository(store)

        repository.save(AutoLockTimeout.Seconds30)

        assertEquals(AutoLockTimeout.Seconds30, repository.autoLockTimeout.first())
        assertEquals("30s", store.data.first()[DataStoreLockSettingsRepository.AUTO_LOCK_TIMEOUT_KEY])
    }

    @Test
    fun `a code this version does not know reads as five minutes`() = runTest {
        val store = store(newFile())
        store.edit { it[DataStoreLockSettingsRepository.AUTO_LOCK_TIMEOUT_KEY] = "1h" }

        assertEquals(AutoLockTimeout.Minutes5, DataStoreLockSettingsRepository(store).autoLockTimeout.first())
    }

    @Test
    fun `leaving does not lock until chosen`() = runTest {
        val repository = DataStoreLockSettingsRepository(store(newFile()))

        assertEquals(false, repository.lockOnLeave.first())
    }

    @Test
    fun `locking on leaving switched on is what is read next`() = runTest {
        val repository = DataStoreLockSettingsRepository(store(newFile()))

        repository.saveLockOnLeave(true)

        assertEquals(true, repository.lockOnLeave.first())
    }

    @Test
    fun `the lock button is not shown until chosen`() = runTest {
        val repository = DataStoreLockSettingsRepository(store(newFile()))

        assertEquals(false, repository.lockButton.first())
    }

    @Test
    fun `the lock button switched on is what is read next`() = runTest {
        val repository = DataStoreLockSettingsRepository(store(newFile()))

        repository.saveLockButton(true)

        assertEquals(true, repository.lockButton.first())
    }

    private fun newFile(): File = File(folder.newFolder(), "app_settings.preferences_pb")

    private fun TestScope.store(file: File): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(scope = backgroundScope) { file }
}
