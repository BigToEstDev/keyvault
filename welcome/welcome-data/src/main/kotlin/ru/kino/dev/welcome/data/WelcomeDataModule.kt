package ru.kino.dev.welcome.data

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.kino.dev.settings.signin.RememberedKeyFile
import ru.kino.dev.welcome.CurrentDatabaseRepository
import ru.kino.dev.welcome.WelcomeDatabaseRepository
import ru.kino.dev.welcome.WelcomeFilesRepository

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

    // The settings change the key file of the remembered database, and nothing else of it (pass-docs, Step 67)
    @Binds
    fun bindRememberedKeyFile(impl: RememberedKeyFileImpl): RememberedKeyFile
}
