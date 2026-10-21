package ru.kino.dev.settings.presets

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.IOException

/** The preset settings in memory; [failSave] - nothing is written, as with a full disk. */
class FakePresetSettingsRepository(
    usePresets: Boolean = true,
    private val failSave: Boolean = false,
) : PresetSettingsRepository {

    private val using = MutableStateFlow(usePresets)

    val savedUsePresets = mutableListOf<Boolean>()

    override val usePresets: Flow<Boolean> = using

    override suspend fun saveUsePresets(on: Boolean) {
        if (failSave) throw IOException("disk full")
        savedUsePresets += on
        using.value = on
    }
}
