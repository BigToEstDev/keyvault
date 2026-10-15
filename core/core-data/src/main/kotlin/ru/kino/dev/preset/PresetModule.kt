package ru.kino.dev.preset

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ru.kino.dev.core.preset.PresetEditor
import ru.kino.dev.core.preset.PresetRepository
import javax.inject.Singleton

/**
 * The presets of one's own: shared by `vault` - the screens of an entry - and later `settings` - the screen
 * of presets, so they live here and not in a feature.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object PresetModule {

    @Provides
    @Singleton
    fun providePresetRepository(@ApplicationContext context: Context): PresetRepository =
        DataStorePresetRepository(context.presetStore)

    // core-domain is plain Kotlin and carries no injection annotations, so its use case is built here
    @Provides
    fun providePresetEditor(presets: PresetRepository): PresetEditor = PresetEditor(presets)
}
