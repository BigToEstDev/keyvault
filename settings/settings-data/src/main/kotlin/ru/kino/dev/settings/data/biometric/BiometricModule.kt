package ru.kino.dev.settings.data.biometric

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.kino.dev.settings.biometric.BiometricCleanup
import ru.kino.dev.settings.biometric.BiometricRepository

/** Binds biometrics to the Keystore and their own store - moved here from `welcome` (pass-docs, Step 59). */
@Module
@InstallIn(SingletonComponent::class)
internal interface BiometricModule {

    // One implementation behind both: screens use biometrics, the repository of welcome forgets stale ones
    @Binds
    fun bindBiometricRepository(impl: BiometricRepositoryImpl): BiometricRepository

    @Binds
    fun bindBiometricCleanup(impl: BiometricRepositoryImpl): BiometricCleanup

    @Binds
    fun bindBiometricKeys(impl: KeystoreBiometricKeys): BiometricKeys

    @Binds
    fun bindBiometricPasswordStore(impl: DataStoreBiometricPasswordStore): BiometricPasswordStore
}
