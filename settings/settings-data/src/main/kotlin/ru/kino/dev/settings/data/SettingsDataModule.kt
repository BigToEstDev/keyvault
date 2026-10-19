package ru.kino.dev.settings.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ru.kino.dev.settings.appearance.AppearanceRepository
import ru.kino.dev.settings.data.appearance.DataStoreAppearanceRepository

/**
 * Builds the repositories of `settings`. The settings of the app share one file, each repository with keys
 * of its own (pass-docs, Step 55).
 */
@Module
@InstallIn(SingletonComponent::class)
internal object SettingsDataModule {

    @Provides
    fun provideAppearanceRepository(@ApplicationContext context: Context): AppearanceRepository =
        DataStoreAppearanceRepository(context.appSettingsStore)
}

// The extension is the documented way to declare a store: one instance per file for the whole process
private val Context.appSettingsStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")
