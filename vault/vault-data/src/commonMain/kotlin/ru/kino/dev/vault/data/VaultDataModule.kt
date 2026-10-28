package ru.kino.dev.vault.data

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import ru.kino.dev.vault.VaultDatabaseRepository
import ru.kino.dev.vault.credentials.DatabaseCredentials
import ru.kino.dev.vault.data.credentials.DatabaseCredentialsImpl
import ru.kino.dev.vault.data.entries.EntrySettingsRepositoryImpl
import ru.kino.dev.vault.data.entry.EntryRepositoryImpl
import ru.kino.dev.vault.data.file.DatabaseFileRepositoryImpl
import ru.kino.dev.vault.data.filter.DataStoreVaultFilterRepository
import ru.kino.dev.vault.data.filter.vaultFilterStore
import ru.kino.dev.vault.data.group.GroupRepositoryImpl
import ru.kino.dev.vault.data.history.EntryHistoryRepositoryImpl
import ru.kino.dev.vault.data.security.SecurityRepositoryImpl
import ru.kino.dev.vault.data.trash.TrashRepositoryImpl
import ru.kino.dev.vault.entries.EntrySettingsEditor
import ru.kino.dev.vault.entries.EntrySettingsRepository
import ru.kino.dev.vault.entry.EntryEditor
import ru.kino.dev.vault.entry.EntryOpener
import ru.kino.dev.vault.entry.EntryRepository
import ru.kino.dev.vault.file.DatabaseFileEditor
import ru.kino.dev.vault.file.DatabaseFileRepository
import ru.kino.dev.vault.filter.VaultFilterRepository
import ru.kino.dev.vault.group.GroupEditor
import ru.kino.dev.vault.group.GroupRepository
import ru.kino.dev.vault.history.EntryHistoryEditor
import ru.kino.dev.vault.history.EntryHistoryReader
import ru.kino.dev.vault.history.EntryHistoryRepository
import ru.kino.dev.vault.history.EntryVersionOpener
import ru.kino.dev.vault.lock.AutoLock
import ru.kino.dev.vault.lock.DatabaseGuard
import ru.kino.dev.vault.search.VaultSearch
import ru.kino.dev.vault.security.SecurityEditor
import ru.kino.dev.vault.security.SecurityRepository
import ru.kino.dev.vault.trash.TrashEditor
import ru.kino.dev.vault.trash.TrashRepository

/** Binds the repositories of `vault` to their implementations, and builds its use cases and its lock. */
val vaultDataModule = module {
    singleOf(::VaultDatabaseRepositoryImpl) { bind<VaultDatabaseRepository>() }
    singleOf(::GroupRepositoryImpl) { bind<GroupRepository>() }
    singleOf(::EntryRepositoryImpl) { bind<EntryRepository>() }
    singleOf(::EntryHistoryRepositoryImpl) { bind<EntryHistoryRepository>() }
    factoryOf(::DatabaseCredentialsImpl) { bind<DatabaseCredentials>() }
    factoryOf(::DatabaseFileRepositoryImpl) { bind<DatabaseFileRepository>() }
    factoryOf(::EntrySettingsRepositoryImpl) { bind<EntrySettingsRepository>() }
    factoryOf(::SecurityRepositoryImpl) { bind<SecurityRepository>() }
    factoryOf(::TrashRepositoryImpl) { bind<TrashRepository>() }

    // vault-domain is plain Kotlin and knows nothing of the graph, so its use cases are built here
    factory { GroupEditor(get(), get(), get()) }
    factory { EntryHistoryEditor(get(), get(), get()) }
    factory { EntryVersionOpener(get(), get()) }
    factory { EntryHistoryReader(get(), get()) }
    factory { EntryOpener(get(), get()) }
    factory { DatabaseFileEditor(get(), get(), get()) }
    factory { EntrySettingsEditor(get(), get(), get()) }
    factory { TrashEditor(get(), get(), get()) }
    factory { SecurityEditor(get(), get(), get(), get(), get()) }
    factory { EntryEditor(get(), get(), get(), get(), get()) }
    factory { VaultSearch(get(), get()) }

    // One for the app: the editors and the lock wait for each other on it
    single { DatabaseGuard() }

    single {
        AutoLock(
            database = get(),
            guard = get(),
            // The count and its time are the settings' (pass-docs, Step 59)
            timer = get(),
            // The app's own: the lock outlives any screen
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
            onFailure = { Log.e(AUTO_LOCK_TAG, "the core refused to lock the database", it) },
        )
    }

    single<VaultFilterRepository> { DataStoreVaultFilterRepository(androidContext().vaultFilterStore) }
}

private const val AUTO_LOCK_TAG = "AutoLock"
