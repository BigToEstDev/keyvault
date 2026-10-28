package ru.kino.dev.core.preset

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * [PresetRepository] in memory, for tests of what is built on it. [failure], when set, is thrown by every
 * change instead of making it.
 */
class FakePresetRepository(vararg presets: EntryPreset) : PresetRepository {

    private val stored = MutableStateFlow(presets.toList())

    var failure: PresetFailure? = null

    override val presets: StateFlow<List<EntryPreset>> = stored

    override suspend fun save(preset: EntryPreset) {
        failure?.let { throw it }
        val list = stored.value
        stored.value = if (list.any { it.id == preset.id }) list.map { if (it.id == preset.id) preset else it } else list + preset
    }

    override suspend fun delete(id: PresetId) {
        failure?.let { throw it }
        stored.value = stored.value.filterNot { it.id == id }
    }
}
