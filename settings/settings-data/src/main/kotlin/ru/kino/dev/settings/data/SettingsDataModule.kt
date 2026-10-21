package ru.kino.dev.settings.data

import android.content.Context
import android.os.SystemClock
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.map
import ru.kino.dev.settings.appearance.AppearanceRepository
import ru.kino.dev.settings.data.appearance.DataStoreAppearanceRepository
import ru.kino.dev.settings.data.lock.ClockInactivityTimer
import ru.kino.dev.settings.data.lock.DataStoreLockSettingsRepository
import ru.kino.dev.settings.data.privacy.DataStorePrivacySettingsRepository
import ru.kino.dev.settings.data.signin.DataStoreSignInSettingsRepository
import ru.kino.dev.settings.lock.InactivityTimer
import ru.kino.dev.settings.lock.LockSettingsRepository
import ru.kino.dev.settings.privacy.PrivacySettingsRepository
import ru.kino.dev.settings.signin.SignInSettingsRepository
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

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

    @Provides
    fun provideLockSettingsRepository(@ApplicationContext context: Context): LockSettingsRepository =
        DataStoreLockSettingsRepository(context.appSettingsStore)

    @Provides
    fun provideSignInSettingsRepository(@ApplicationContext context: Context): SignInSettingsRepository =
        DataStoreSignInSettingsRepository(context.appSettingsStore)

    @Provides
    fun providePrivacySettingsRepository(@ApplicationContext context: Context): PrivacySettingsRepository =
        DataStorePrivacySettingsRepository(context.appSettingsStore)

    // One for the app: the app tells it what it sees, `vault` listens (pass-docs, Step 59)
    @Provides
    @Singleton
    fun provideInactivityTimer(settings: LockSettingsRepository): InactivityTimer = ClockInactivityTimer(
        // Counts while the phone sleeps, unlike the clock coroutines wait by
        clock = { SystemClock.elapsedRealtime().milliseconds },
        // The app's own: the count outlives any screen
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
        timeouts = settings.autoLockTimeout.map { it.duration },
        lockOnLeave = settings.lockOnLeave,
    )
}

// The extension is the documented way to declare a store: one instance per file for the whole process
private val Context.appSettingsStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")
