package ru.kino.dev.vault.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import ru.kino.dev.log.ErrorLog
import ru.kino.dev.storage.AppDataDir
import ru.kino.dev.storage.preferencesStore
import ru.kino.dev.vault.VaultDatabaseRepository
import ru.kino.dev.vault.attachment.AttachmentFiles
import ru.kino.dev.vault.attachment.AttachmentRepository
import ru.kino.dev.vault.changes.VaultChanges
import ru.kino.dev.vault.credentials.DatabaseCredentials
import ru.kino.dev.vault.data.attachment.AttachmentRepositoryImpl
import ru.kino.dev.vault.data.changes.VaultChangesImpl
import ru.kino.dev.vault.data.credentials.DatabaseCredentialsImpl
import ru.kino.dev.vault.data.entries.EntrySettingsRepositoryImpl
import ru.kino.dev.vault.data.entry.EntryRepositoryImpl
import ru.kino.dev.vault.data.file.DatabaseFileRepositoryImpl
import ru.kino.dev.vault.data.filter.DataStoreVaultFilterRepository
import ru.kino.dev.vault.data.generator.PasswordGeneratorImpl
import ru.kino.dev.vault.data.group.GroupRepositoryImpl
import ru.kino.dev.vault.data.history.EntryHistoryRepositoryImpl
import ru.kino.dev.vault.data.security.SecurityRepositoryImpl
import ru.kino.dev.vault.data.sync.OtherDatabaseRepositoryImpl
import ru.kino.dev.vault.data.trash.TrashRepositoryImpl
import ru.kino.dev.vault.entries.EntrySettingsEditor
import ru.kino.dev.vault.entries.EntrySettingsRepository
import ru.kino.dev.vault.entry.EntryEditor
import ru.kino.dev.vault.entry.EntryOpener
import ru.kino.dev.vault.entry.EntryRepository
import ru.kino.dev.vault.file.DatabaseFileEditor
import ru.kino.dev.vault.file.DatabaseFileRepository
import ru.kino.dev.vault.filter.VaultFilterRepository
import ru.kino.dev.vault.generator.PasswordGenerator
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
import ru.kino.dev.vault.sync.OtherDatabaseRepository
import ru.kino.dev.vault.sync.AppReturns
import ru.kino.dev.vault.sync.SaveStatus
import ru.kino.dev.vault.sync.SyncRequests
import ru.kino.dev.vault.sync.VaultSync
import ru.kino.dev.vault.trash.TrashEditor
import ru.kino.dev.vault.trash.TrashRepository

private const val VAULT_FILTER_STORE = "vault_filter"

/** Binds the repositories of `vault` to their implementations, and builds its use cases and its lock. */
val vaultDataModule = module {
    // One for the app: the repositories move the versions, the screens listen (pass-docs, Step 98)
    singleOf(::VaultChangesImpl) { bind<VaultChanges>() }
    singleOf(::VaultDatabaseRepositoryImpl) { bind<VaultDatabaseRepository>() }
    singleOf(::GroupRepositoryImpl) { bind<GroupRepository>() }
    singleOf(::EntryRepositoryImpl) { bind<EntryRepository>() }
    singleOf(::EntryHistoryRepositoryImpl) { bind<EntryHistoryRepository>() }
    factoryOf(::AttachmentRepositoryImpl) { bind<AttachmentRepository>() }
    factoryOf(::PasswordGeneratorImpl) { bind<PasswordGenerator>() }
    factoryOf(::DatabaseCredentialsImpl) { bind<DatabaseCredentials>() }
    factoryOf(::DatabaseFileRepositoryImpl) { bind<DatabaseFileRepository>() }
    factoryOf(::EntrySettingsRepositoryImpl) { bind<EntrySettingsRepository>() }
    factoryOf(::SecurityRepositoryImpl) { bind<SecurityRepository>() }
    factoryOf(::TrashRepositoryImpl) { bind<TrashRepository>() }
    factoryOf(::OtherDatabaseRepositoryImpl) { bind<OtherDatabaseRepository>() }

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
    factory { AttachmentFiles(get(), get()) }

    // One for the app: the editors and the lock wait for each other on it
    single { DatabaseGuard() }

    // One for the app: the save sets it, the sync and the screens read it (pass-docs, Step 108)
    single { SaveStatus() }
    single { SyncRequests() }
    // The app tells it it came back; the quiet check of the sync listens
    single { AppReturns() }
    factory { VaultSync(get(), get(), get()) }

    single {
        val log = get<ErrorLog>()
        AutoLock(
            database = get(),
            guard = get(),
            // The count and its time are the settings' (pass-docs, Step 59)
            timer = get(),
            // The app's own: the lock outlives any screen
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
            // The core refused to lock the database (pass-docs, Step 86: into ErrorLog, not android.util.Log)
            onFailure = { log.unexpected(AUTO_LOCK_TAG, it) },
        )
    }

    // One store per file for the whole process (pass-docs, Step 86)
    single(named(VAULT_FILTER_STORE)) { get<AppDataDir>().preferencesStore(VAULT_FILTER_STORE) }

    single<VaultFilterRepository> { DataStoreVaultFilterRepository(get(named(VAULT_FILTER_STORE))) }
}

private const val AUTO_LOCK_TAG = "AutoLock"
