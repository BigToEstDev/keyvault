package ru.kino.dev.di

import org.koin.core.module.Module
import org.koin.dsl.module
import ru.kino.dev.core.DatabaseFiles
import ru.kino.dev.storage.AppDataDir
import ru.kino.dev.storage.DesktopAppDataDir
import ru.kino.dev.storage.FileDatabaseFiles

/** The files of the desktop: databases by their path, the settings in the data directory of the user. */
internal actual val platformCoreDataModule: Module = module {
    single<DatabaseFiles> { FileDatabaseFiles() }
    single<AppDataDir> { DesktopAppDataDir() }
}
