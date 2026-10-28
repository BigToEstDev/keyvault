package ru.kino.dev.settings.data.biometric

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.binds
import org.koin.dsl.module
import ru.kino.dev.settings.biometric.BiometricCleanup
import ru.kino.dev.settings.biometric.BiometricRepository

/** Binds biometrics to the Keystore and their own store - moved here from `welcome` (pass-docs, Step 59). */
internal val biometricModule = module {
    // One implementation behind both: screens use biometrics, the repository of welcome forgets stale ones
    single { BiometricRepositoryImpl(get(), get()) } binds
        arrayOf(BiometricRepository::class, BiometricCleanup::class)

    factory<BiometricKeys> { KeystoreBiometricKeys(androidContext()) }

    single<BiometricPasswordStore> { DataStoreBiometricPasswordStore(androidContext()) }
}
