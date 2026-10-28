package ru.kino.dev.settings.data

import android.os.SystemClock
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.binds
import org.koin.dsl.module
import ru.kino.dev.settings.biometric.BiometricCleanup
import ru.kino.dev.settings.biometric.BiometricRepository
import ru.kino.dev.settings.data.biometric.BiometricKeys
import ru.kino.dev.settings.data.biometric.BiometricPasswordStore
import ru.kino.dev.settings.data.biometric.BiometricRepositoryImpl
import ru.kino.dev.settings.data.biometric.DataStoreBiometricPasswordStore
import ru.kino.dev.settings.data.biometric.KeystoreBiometricKeys
import ru.kino.dev.storage.AppDataDir
import ru.kino.dev.storage.preferencesStore
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private const val BIOMETRIC_PASSWORD_STORE = "biometric_password"

/** Binds biometrics to the Keystore and their own store - moved here from `welcome` (pass-docs, Step 59). */
internal actual val platformSettingsDataModule: Module = module {
    // One implementation behind both: screens use biometrics, the repository of welcome forgets stale ones
    single { BiometricRepositoryImpl(get(), get()) } binds
        arrayOf(BiometricRepository::class, BiometricCleanup::class)

    factory<BiometricKeys> { KeystoreBiometricKeys(androidContext()) }

    // One store per file for the whole process (pass-docs, Step 86)
    single(named(BIOMETRIC_PASSWORD_STORE)) { get<AppDataDir>().preferencesStore(BIOMETRIC_PASSWORD_STORE) }

    single<BiometricPasswordStore> { DataStoreBiometricPasswordStore(get(named(BIOMETRIC_PASSWORD_STORE))) }
}

// Counts while the phone sleeps, unlike System.nanoTime
internal actual fun sinceBoot(): Duration = SystemClock.elapsedRealtime().milliseconds
