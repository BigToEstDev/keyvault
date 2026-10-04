package ru.kino.dev.welcome.data

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.kino.dev.welcome.BiometricRepository
import ru.kino.dev.welcome.CurrentDatabaseRepository
import ru.kino.dev.welcome.WelcomeDatabaseRepository
import ru.kino.dev.welcome.WelcomeFilesRepository
import ru.kino.dev.welcome.data.biometric.BiometricCleanup
import ru.kino.dev.welcome.data.biometric.BiometricKeys
import ru.kino.dev.welcome.data.biometric.BiometricPasswordStore
import ru.kino.dev.welcome.data.biometric.BiometricRepositoryImpl
import ru.kino.dev.welcome.data.biometric.DataStoreBiometricPasswordStore
import ru.kino.dev.welcome.data.biometric.KeystoreBiometricKeys

/** Binds the repositories of `welcome` to their implementations. */
@Module
@InstallIn(SingletonComponent::class)
internal interface WelcomeDataModule {

    @Binds
    fun bindWelcomeDatabaseRepository(impl: WelcomeDatabaseRepositoryImpl): WelcomeDatabaseRepository

    @Binds
    fun bindWelcomeFilesRepository(impl: WelcomeFilesRepositoryImpl): WelcomeFilesRepository

    // One store behind both: screens read it, only the repository above writes it
    @Binds
    fun bindCurrentDatabaseRepository(impl: CurrentDatabaseStore): CurrentDatabaseRepository

    @Binds
    fun bindCurrentDatabaseWriter(impl: CurrentDatabaseStore): CurrentDatabaseWriter

    // One implementation behind both: screens use biometrics, the repository above forgets stale ones
    @Binds
    fun bindBiometricRepository(impl: BiometricRepositoryImpl): BiometricRepository

    @Binds
    fun bindBiometricCleanup(impl: BiometricRepositoryImpl): BiometricCleanup

    @Binds
    fun bindBiometricKeys(impl: KeystoreBiometricKeys): BiometricKeys

    @Binds
    fun bindBiometricPasswordStore(impl: DataStoreBiometricPasswordStore): BiometricPasswordStore
}
