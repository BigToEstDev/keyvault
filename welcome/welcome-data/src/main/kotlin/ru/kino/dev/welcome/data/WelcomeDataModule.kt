package ru.kino.dev.welcome.data

import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.binds
import org.koin.dsl.module
import ru.kino.dev.settings.signin.RememberedKeyFile
import ru.kino.dev.welcome.CurrentDatabaseRepository
import ru.kino.dev.welcome.WelcomeDatabaseRepository
import ru.kino.dev.welcome.WelcomeFilesRepository

/** Binds the repositories of `welcome` to their implementations. */
val welcomeDataModule = module {
    singleOf(::WelcomeDatabaseRepositoryImpl) { bind<WelcomeDatabaseRepository>() }

    singleOf(::WelcomeFilesRepositoryImpl) { bind<WelcomeFilesRepository>() }

    // One store behind both: screens read it, only the repository above writes it
    single { CurrentDatabaseStore(androidContext()) } binds
        arrayOf(CurrentDatabaseRepository::class, CurrentDatabaseWriter::class)

    // The settings change the key file of the remembered database, and nothing else of it (pass-docs, Step 67)
    factoryOf(::RememberedKeyFileImpl) { bind<RememberedKeyFile>() }
}
