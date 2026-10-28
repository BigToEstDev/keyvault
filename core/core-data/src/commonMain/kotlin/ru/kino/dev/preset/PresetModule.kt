package ru.kino.dev.preset

import org.koin.core.qualifier.named
import org.koin.dsl.module
import ru.kino.dev.core.preset.PresetEditor
import ru.kino.dev.core.preset.PresetRepository
import ru.kino.dev.storage.AppDataDir
import ru.kino.dev.storage.preferencesStore

private const val PRESETS_STORE = "presets"

/**
 * The presets of one's own: shared by `vault` - the screens of an entry - and `settings` - the screen of
 * presets, so they live here and not in a feature.
 */
internal val presetModule = module {
    // One store per file for the whole process (pass-docs, Step 86)
    single(named(PRESETS_STORE)) { get<AppDataDir>().preferencesStore(PRESETS_STORE) }

    single<PresetRepository> { DataStorePresetRepository(get(named(PRESETS_STORE))) }

    // core-domain is plain Kotlin and knows nothing of the graph, so its use case is built here
    factory { PresetEditor(get()) }
}
