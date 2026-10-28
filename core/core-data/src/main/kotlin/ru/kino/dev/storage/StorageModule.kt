package ru.kino.dev.storage

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import ru.kino.dev.core.DatabaseFiles

/**
 * Binds the storage the features read and write their files through.
 *
 * The repositories themselves live in the features (`welcome`, `vault`); what stays here is the part they
 * share - the storage access framework.
 */
internal val storageModule = module {
    single<DatabaseFiles> { SafDatabaseFiles(androidContext()) }
}
