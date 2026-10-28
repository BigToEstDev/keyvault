package ru.kino.dev.preset

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import ru.kino.dev.core.TemporaryFolder
import ru.kino.dev.core.preset.CustomPreset
import ru.kino.dev.core.preset.EntryPreset
import ru.kino.dev.core.preset.PresetId

class DataStorePresetRepositoryTest {

    private val folder = TemporaryFolder()

    @AfterTest
    fun deleteFolder() = folder.delete()

    private val card = CustomPreset.of(PresetId("card"), "Card", listOf(listOf("PIN"), listOf("Number", "CVC")))
    private val wifi = CustomPreset.of(PresetId("wifi"), "Wi-Fi", listOf(emptyList(), listOf("SSID")))

    @Test
    fun `nothing saved yet is no presets`() = runTest {
        assertEquals(emptyList<EntryPreset>(), DataStorePresetRepository(MemoryStore()).presets.first())
    }

    @Test
    fun `presets saved are read back whole, in the order they were made`() = runTest {
        val repository = DataStorePresetRepository(MemoryStore())

        repository.save(card)
        repository.save(wifi)

        assertEquals(listOf(card, wifi), repository.presets.first())
    }

    @Test
    fun `a preset saved again takes the place of the one with its id`() = runTest {
        val repository = DataStorePresetRepository(MemoryStore())
        repository.save(card)
        repository.save(wifi)

        val renamed = CustomPreset.of(PresetId("card"), "Bank card", listOf(listOf("PIN")))
        repository.save(renamed)

        assertEquals(listOf(renamed, wifi), repository.presets.first())
    }

    @Test
    fun `a preset is deleted, the rest stay`() = runTest {
        val repository = DataStorePresetRepository(MemoryStore())
        repository.save(card)
        repository.save(wifi)

        repository.delete(PresetId("card"))

        assertEquals(listOf(wifi), repository.presets.first())
    }

    @Test
    fun `presets saved are read again by a new store over the same file - a new launch`() = runTest {
        val file = newFile()

        // One store per file at a time: the first one is closed before the second opens the file
        val firstLaunch = Job()
        DataStorePresetRepository(store(file, CoroutineScope(firstLaunch + StandardTestDispatcher(testScheduler))))
            .save(card)
        firstLaunch.cancel()
        testScheduler.advanceUntilIdle()

        assertEquals(listOf(card), DataStorePresetRepository(store(file)).presets.first())
    }

    @Test
    fun `only what the person set is kept - the names, by version`() = runTest {
        val store = MemoryStore()
        DataStorePresetRepository(store).save(card)

        assertEquals(
            """{"version":1,"presets":[{"id":"card","name":"Card","cards":[["PIN"],["Number","CVC"]]}]}""",
            store.data.first()[DataStorePresetRepository.PRESETS_KEY],
        )
    }

    @Test
    fun `the protected fields are kept by name in the same version, and read back`() = runTest {
        val protectedCard = CustomPreset.of(PresetId("card"), "Card", listOf(listOf("PIN"), listOf("Number", "CVC")), setOf("CVC"))
        val store = MemoryStore()
        DataStorePresetRepository(store).save(protectedCard)

        assertEquals(
            """{"version":1,"presets":[{"id":"card","name":"Card","cards":[["PIN"],["Number","CVC"]],"protectedKeys":["CVC"]}]}""",
            store.data.first()[DataStorePresetRepository.PRESETS_KEY],
        )
        assertEquals(listOf(protectedCard), DataStorePresetRepository(store).presets.first())
    }

    @Test
    fun `the types of the fields are kept by name in the same version, and read back`() = runTest {
        val typedCard = CustomPreset.of(
            PresetId("card"),
            "Card",
            listOf(listOf("PIN"), listOf("Number", "CVC")),
            fieldTypes = mapOf("Number" to "card"),
        )
        val store = MemoryStore()
        DataStorePresetRepository(store).save(typedCard)

        assertEquals(
            """{"version":1,"presets":[{"id":"card","name":"Card","cards":[["PIN"],["Number","CVC"]],"fieldTypes":{"Number":"card"}}]}""",
            store.data.first()[DataStorePresetRepository.PRESETS_KEY],
        )
        assertEquals(listOf(typedCard), DataStorePresetRepository(store).presets.first())
    }

    @Test
    fun `json that does not parse is no presets`() = runTest {
        assertEquals(emptyList<EntryPreset>(), stored("{not json").presets.first())
    }

    @Test
    fun `a version this app does not know is no presets`() = runTest {
        assertEquals(emptyList<EntryPreset>(), stored("""{"version":2,"presets":[]}""").presets.first())
    }

    @Test
    fun `a field a newer app added is skipped`() = runTest {
        val repository = stored("""{"version":1,"presets":[{"id":"card","name":"Card","cards":[["PIN"],["Number","CVC"]],"icon":7}]}""")

        assertEquals(listOf(card), repository.presets.first())
    }

    @Test
    fun `a preset that does not build is left out alone`() = runTest {
        val repository = stored(
            """{"version":1,"presets":[{"id":"bad","name":"Bad","cards":[["PIN","PIN"]]},""" +
                """{"id":"card","name":"Card","cards":[["PIN"],["Number","CVC"]]}]}""",
        )

        assertEquals(listOf(card), repository.presets.first())
    }

    private suspend fun stored(json: String): DataStorePresetRepository {
        val store = MemoryStore()
        store.edit { it[DataStorePresetRepository.PRESETS_KEY] = json }
        return DataStorePresetRepository(store)
    }

    /**
     * A store in memory, for what is not about the file. On Windows a second write in a row of a store on a
     * file fails to rename its temporary file over the first - a quirk of the desktop jvm, not of Android.
     */
    private class MemoryStore : DataStore<Preferences> {
        private val state = MutableStateFlow(emptyPreferences())
        private val lock = Mutex()

        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
            lock.withLock { transform(state.value).toPreferences().also { state.value = it } }
    }

    /** A path for a store - the file itself is not there yet, as on a first launch. */
    private fun newFile(): File = File(folder.root, "presets-${files++}.preferences_pb")

    private var files = 0

    private fun TestScope.store(file: File, scope: CoroutineScope = backgroundScope): DataStore<Preferences> =
        PreferenceDataStoreFactory.createWithPath(scope = scope) { file.absolutePath.toPath() }
}
