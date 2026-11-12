package ru.kino.dev.settings.data

import kotlin.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.map
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import ru.kino.dev.settings.about.CoreVersionRepository
import ru.kino.dev.settings.appearance.AppearanceRepository
import ru.kino.dev.settings.data.about.NativeCoreVersionRepository
import ru.kino.dev.settings.data.appearance.DataStoreAppearanceRepository
import ru.kino.dev.settings.data.lock.ClockInactivityTimer
import ru.kino.dev.settings.data.lock.DataStoreLockSettingsRepository
import ru.kino.dev.settings.data.presets.DataStorePresetSettingsRepository
import ru.kino.dev.settings.data.privacy.DataStorePrivacySettingsRepository
import ru.kino.dev.settings.data.signin.DataStoreSignInSettingsRepository
import ru.kino.dev.settings.lock.InactivityTimer
import ru.kino.dev.settings.lock.LockSettingsRepository
import ru.kino.dev.settings.presets.PresetSettingsRepository
import ru.kino.dev.settings.privacy.PrivacySettingsRepository
import ru.kino.dev.settings.signin.SignInSettingsRepository
import ru.kino.dev.storage.AppDataDir
import ru.kino.dev.storage.preferencesStore

private const val APP_SETTINGS_STORE = "app_settings"

/**
 * Builds the repositories of `settings`. The settings of the app share one file, each repository with keys
 * of its own (pass-docs, Step 55).
 */
val settingsDataModule = module {
    includes(platformSettingsDataModule)

    // One store per file for the whole process, the five repositories below share it (pass-docs, Step 86)
    single(named(APP_SETTINGS_STORE)) { get<AppDataDir>().preferencesStore(APP_SETTINGS_STORE) }

    // The version of the core for the screen "About" (pass-docs, Step 123)
    factory<CoreVersionRepository> { NativeCoreVersionRepository(get()) }

    factory<AppearanceRepository> { DataStoreAppearanceRepository(get(named(APP_SETTINGS_STORE))) }

    factory<LockSettingsRepository> { DataStoreLockSettingsRepository(get(named(APP_SETTINGS_STORE))) }

    factory<SignInSettingsRepository> { DataStoreSignInSettingsRepository(get(named(APP_SETTINGS_STORE))) }

    factory<PrivacySettingsRepository> { DataStorePrivacySettingsRepository(get(named(APP_SETTINGS_STORE))) }

    factory<PresetSettingsRepository> { DataStorePresetSettingsRepository(get(named(APP_SETTINGS_STORE))) }

    // One for the app: the app tells it what it sees, `vault` listens (pass-docs, Step 59)
    single<InactivityTimer> {
        val settings = get<LockSettingsRepository>()
        ClockInactivityTimer(
            // Counts while the phone sleeps, unlike the clock coroutines wait by - see sinceBoot
            clock = ::sinceBoot,
            // The app's own: the count outlives any screen
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
            timeouts = settings.autoLockTimeout.map { it.duration },
            lockOnLeave = settings.lockOnLeave,
        )
    }
}

/** Biometrics of the platform: the Keystore on android, none on the desktop yet (pass-docs). */
internal expect val platformSettingsDataModule: Module

/**
 * A monotonic clock for the timer of inactivity. On android it goes on while the phone sleeps - the lock must
 * come after a night in the pocket too - so not the clock coroutines wait by.
 */
internal expect fun sinceBoot(): Duration
