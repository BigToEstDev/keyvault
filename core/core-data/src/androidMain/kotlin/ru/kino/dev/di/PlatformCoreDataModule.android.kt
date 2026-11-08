package ru.kino.dev.di

import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import ru.kino.dev.core.DatabaseFiles
import ru.kino.dev.core.SharedFiles
import ru.kino.dev.storage.AndroidAppDataDir
import ru.kino.dev.storage.AndroidSharedFiles
import ru.kino.dev.storage.AppDataDir
import ru.kino.dev.storage.SafDatabaseFiles

/**
 * The files of android: databases through the storage access framework - the repositories themselves live in the
 * features, what stays here is the part they share - `filesDir` for the settings, `cache/shared` for files sent.
 */
internal actual val platformCoreDataModule: Module = module {
    single<DatabaseFiles> { SafDatabaseFiles(androidContext()) }
    single<AppDataDir> { AndroidAppDataDir(androidContext()) }
    // Files handed to other apps - "Send" (pass-docs, Step 111)
    single<SharedFiles> { AndroidSharedFiles(androidContext()) }
}
