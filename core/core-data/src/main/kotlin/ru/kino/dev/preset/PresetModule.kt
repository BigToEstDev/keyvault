package ru.kino.dev.preset

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import ru.kino.dev.core.preset.PresetEditor
import ru.kino.dev.core.preset.PresetRepository

/**
 * The presets of one's own: shared by `vault` - the screens of an entry - and `settings` - the screen of
 * presets, so they live here and not in a feature.
 */
internal val presetModule = module {
    single<PresetRepository> { DataStorePresetRepository(androidContext().presetStore) }

    // core-domain is plain Kotlin and knows nothing of the graph, so its use case is built here
    factory { PresetEditor(get()) }
}
