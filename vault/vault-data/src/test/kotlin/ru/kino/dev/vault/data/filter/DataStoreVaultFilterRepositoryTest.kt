package ru.kino.dev.vault.data.filter

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
import ru.kino.dev.vault.filter.CreationOrder
import ru.kino.dev.vault.filter.VaultFilter
import ru.kino.dev.vault.filter.Visibility
import java.io.File

class DataStoreVaultFilterRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun `nothing chosen yet reads as the default`() = runTest {
        val repository = DataStoreVaultFilterRepository(store(newFile()))

        assertEquals(VaultFilter.DEFAULT, repository.filter.first())
    }

    @Test
    fun `a saved filter is what is read next`() = runTest {
        val repository = DataStoreVaultFilterRepository(store(newFile()))
        val chosen = VaultFilter(visibility = Visibility.EntriesOnly, order = CreationOrder.OldestFirst)

        repository.save(chosen)

        assertEquals(chosen, repository.filter.first())
    }

    @Test
    fun `a saved filter is read again by a new store over the same file - a new launch`() = runTest {
        val file = newFile()
        val chosen = VaultFilter(visibility = Visibility.GroupsOnly)

        // One store per file at a time: the first one is closed before the second opens the file
        val firstLaunch = Job()
        DataStoreVaultFilterRepository(store(file, CoroutineScope(firstLaunch + StandardTestDispatcher(testScheduler))))
            .save(chosen)
        firstLaunch.cancel()
        testScheduler.advanceUntilIdle()

        assertEquals(chosen, DataStoreVaultFilterRepository(store(file)).filter.first())
    }

    @Test
    fun `the choices are kept as codes of their own, not as names of the enums`() = runTest {
        val store = store(newFile())
        DataStoreVaultFilterRepository(store)
            .save(VaultFilter(visibility = Visibility.GroupsOnly, order = CreationOrder.OldestFirst))

        val stored = store.data.first()
        assertEquals("groups", stored[DataStoreVaultFilterRepository.VISIBILITY_KEY])
        assertEquals("oldest", stored[DataStoreVaultFilterRepository.ORDER_KEY])
    }

    @Test
    fun `a code this version does not know reads as the default of that choice`() = runTest {
        val store = store(newFile())
        store.edit {
            it[DataStoreVaultFilterRepository.VISIBILITY_KEY] = "favourites"
            it[DataStoreVaultFilterRepository.ORDER_KEY] = "oldest"
        }

        assertEquals(
            VaultFilter(visibility = Visibility.All, order = CreationOrder.OldestFirst),
            DataStoreVaultFilterRepository(store).filter.first(),
        )
    }

    /** A path for a store - the file itself is not there yet, as on a first launch. */
    private fun newFile(): File = File(folder.root, "filter-${files++}.preferences_pb")

    private var files = 0

    private fun TestScope.store(file: File, scope: CoroutineScope = backgroundScope): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(scope = scope) { file }
}
