package ru.kino.dev.settings.data.appearance

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import ru.kino.dev.settings.appearance.AppLanguage
import ru.kino.dev.settings.appearance.ThemeMode
import java.io.File

class DataStoreAppearanceRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun `nothing chosen yet reads as the system mode`() = runTest {
        val repository = DataStoreAppearanceRepository(store(newFile()))

        assertEquals(ThemeMode.System, repository.themeMode.first())
    }

    @Test
    fun `a saved mode is what is read next`() = runTest {
        val repository = DataStoreAppearanceRepository(store(newFile()))

        repository.save(ThemeMode.Dark)

        assertEquals(ThemeMode.Dark, repository.themeMode.first())
    }

    @Test
    fun `a saved mode is read again by a new store over the same file - a new launch`() = runTest {
        val file = newFile()

        // One store per file at a time: the first one is closed before the second opens the file
        val firstLaunch = Job()
        DataStoreAppearanceRepository(store(file, CoroutineScope(firstLaunch + StandardTestDispatcher(testScheduler))))
            .save(ThemeMode.Light)
        firstLaunch.cancel()
        testScheduler.advanceUntilIdle()

        assertEquals(ThemeMode.Light, DataStoreAppearanceRepository(store(file)).themeMode.first())
    }

    @Test
    fun `the mode is kept as a code of its own, not as the name of the enum`() = runTest {
        val store = store(newFile())

        DataStoreAppearanceRepository(store).save(ThemeMode.Dark)

        assertEquals("dark", store.data.first()[DataStoreAppearanceRepository.THEME_MODE_KEY])
    }

    @Test
    fun `a code this version does not know reads as the system mode`() = runTest {
        val store = store(newFile())
        store.edit { it[DataStoreAppearanceRepository.THEME_MODE_KEY] = "sepia" }

        assertEquals(ThemeMode.System, DataStoreAppearanceRepository(store).themeMode.first())
    }

    @Test
    fun `no language chosen yet reads as the system one`() = runTest {
        val repository = DataStoreAppearanceRepository(store(newFile()))

        assertEquals(AppLanguage.System, repository.language.first())
    }

    @Test
    fun `a saved language is what is read next, kept as a code of its own`() = runTest {
        val store = store(newFile())
        val repository = DataStoreAppearanceRepository(store)

        repository.save(AppLanguage.Russian)

        assertEquals(AppLanguage.Russian, repository.language.first())
        assertEquals("ru", store.data.first()[DataStoreAppearanceRepository.LANGUAGE_KEY])
    }

    @Test
    fun `a language code this version does not know reads as the system one`() = runTest {
        val store = store(newFile())
        store.edit { it[DataStoreAppearanceRepository.LANGUAGE_KEY] = "fr" }

        assertEquals(AppLanguage.System, DataStoreAppearanceRepository(store).language.first())
    }

    @Test
    fun `passwords are colored until chosen otherwise`() = runTest {
        val repository = DataStoreAppearanceRepository(store(newFile()))

        assertEquals(true, repository.passwordColoring.first())
    }

    @Test
    fun `coloring switched off is what is read next`() = runTest {
        val repository = DataStoreAppearanceRepository(store(newFile()))

        repository.savePasswordColoring(false)

        assertEquals(false, repository.passwordColoring.first())
    }

    @Test
    fun `user names are not in the list until chosen`() = runTest {
        val repository = DataStoreAppearanceRepository(store(newFile()))

        assertEquals(false, repository.userNameInList.first())
    }

    @Test
    fun `user names switched on are what is read next`() = runTest {
        val repository = DataStoreAppearanceRepository(store(newFile()))

        repository.saveUserNameInList(true)

        assertEquals(true, repository.userNameInList.first())
    }

    private fun newFile(): File = File(folder.newFolder(), "app_settings.preferences_pb")

    private fun TestScope.store(
        file: File,
        scope: CoroutineScope = backgroundScope,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(scope = scope) { file }
}
