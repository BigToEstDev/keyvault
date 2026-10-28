package ru.kino.dev.preset

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import ru.kino.dev.core.preset.EntryPreset
import ru.kino.dev.core.preset.PresetFailure
import ru.kino.dev.core.preset.PresetId
import ru.kino.dev.core.preset.PresetRepository
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

/**
 * The presets of one's own on a preferences data store - one for the app, not one per database (pass-docs,
 * Step 44, answer 4). The list is one json string ([PresetsDto]).
 *
 * The store is not encrypted: it holds the names of presets and of their fields, never a value of an entry.
 *
 * What cannot be read - a file the store fails on, json that does not parse, a [PresetsDto.version] this app
 * does not know - reads as no presets: entries open with the standard one. A single preset that does not
 * build - two fields under one key, say - is left out alone. A change then writes the list as this app reads
 * it: an app put back to an older version loses presets of the newer one.
 */
internal class DataStorePresetRepository(
    private val store: DataStore<Preferences>,
) : PresetRepository {

    override val presets: Flow<List<EntryPreset>> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { decode(it[PRESETS_KEY]) }
        .distinctUntilChanged()

    override suspend fun save(preset: EntryPreset) = change { list ->
        if (list.any { it.id == preset.id }) list.map { if (it.id == preset.id) preset else it } else list + preset
    }

    override suspend fun delete(id: PresetId) = change { list -> list.filterNot { it.id == id } }

    private suspend fun change(transform: (List<EntryPreset>) -> List<EntryPreset>) {
        try {
            store.edit { preferences ->
                val changed = transform(decode(preferences[PRESETS_KEY]))
                preferences[PRESETS_KEY] = JSON.encodeToString(PresetsDto(VERSION, changed.map { it.toDto() }))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            throw PresetFailure.NotSaved(e)
        }
    }

    private fun decode(stored: String?): List<EntryPreset> {
        if (stored == null) return emptyList()
        val dto = try {
            JSON.decodeFromString<PresetsDto>(stored)
        } catch (_: SerializationException) {
            return emptyList()
        } catch (_: IllegalArgumentException) {
            return emptyList()
        }
        if (dto.version != VERSION) return emptyList()
        return dto.presets.mapNotNull {
            try {
                it.toPreset()
            } catch (_: IllegalArgumentException) {
                null
            }
        }
    }

    internal companion object {
        val PRESETS_KEY: Preferences.Key<String> = stringPreferencesKey("presets")

        /** The shape of [PresetsDto] this app writes and reads. */
        const val VERSION = 1

        // A field added by a newer app of the same version is skipped, not a reason to lose every preset
        private val JSON = Json { ignoreUnknownKeys = true }
    }
}
