package ru.kino.dev.vault.data

import android.content.Context
import android.util.Log
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import ru.kino.dev.core.preset.PresetRepository
import ru.kino.dev.settings.lock.InactivityTimer
import ru.kino.dev.vault.VaultDatabaseRepository
import ru.kino.dev.vault.data.entry.EntryRepositoryImpl
import ru.kino.dev.vault.data.filter.DataStoreVaultFilterRepository
import ru.kino.dev.vault.data.filter.vaultFilterStore
import ru.kino.dev.vault.data.group.GroupRepositoryImpl
import ru.kino.dev.vault.data.history.EntryHistoryRepositoryImpl
import ru.kino.dev.vault.entry.EntryEditor
import ru.kino.dev.vault.entry.EntryOpener
import ru.kino.dev.vault.entry.EntryRepository
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
import javax.inject.Singleton

/** Binds the repositories of `vault` to their implementations, and builds its use cases and its lock. */
@Module
@InstallIn(SingletonComponent::class)
internal interface VaultDataModule {

    @Binds
    fun bindVaultDatabaseRepository(impl: VaultDatabaseRepositoryImpl): VaultDatabaseRepository

    @Binds
    fun bindGroupRepository(impl: GroupRepositoryImpl): GroupRepository

    @Binds
    fun bindEntryRepository(impl: EntryRepositoryImpl): EntryRepository

    @Binds
    fun bindEntryHistoryRepository(impl: EntryHistoryRepositoryImpl): EntryHistoryRepository

    companion object {

        // vault-domain is plain Kotlin and carries no injection annotations, so its use cases are built here
        @Provides
        fun provideGroupEditor(
            groups: GroupRepository,
            database: VaultDatabaseRepository,
            guard: DatabaseGuard,
        ): GroupEditor = GroupEditor(groups, database, guard)

        @Provides
        fun provideEntryHistoryEditor(
            history: EntryHistoryRepository,
            database: VaultDatabaseRepository,
            guard: DatabaseGuard,
        ): EntryHistoryEditor = EntryHistoryEditor(history, database, guard)

        @Provides
        fun provideEntryVersionOpener(history: EntryHistoryRepository, opener: EntryOpener): EntryVersionOpener =
            EntryVersionOpener(history, opener)

        @Provides
        fun provideEntryHistoryReader(entries: EntryRepository, history: EntryHistoryRepository): EntryHistoryReader =
            EntryHistoryReader(entries, history)

        @Provides
        fun provideEntryOpener(entries: EntryRepository, presets: PresetRepository): EntryOpener =
            EntryOpener(entries, presets)

        @Provides
        fun provideEntryEditor(
            entries: EntryRepository,
            opener: EntryOpener,
            database: VaultDatabaseRepository,
            guard: DatabaseGuard,
        ): EntryEditor = EntryEditor(entries, opener, database, guard)

        // One for the app: the editors and the lock wait for each other on it
        @Provides
        @Singleton
        fun provideDatabaseGuard(): DatabaseGuard = DatabaseGuard()

        @Provides
        @Singleton
        fun provideAutoLock(
            database: VaultDatabaseRepository,
            guard: DatabaseGuard,
            timer: InactivityTimer,
        ): AutoLock = AutoLock(
            database = database,
            guard = guard,
            // The count and its time are the settings' (pass-docs, Step 59)
            timer = timer,
            // The app's own: the lock outlives any screen
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
            onFailure = { Log.e(TAG, "the core refused to lock the database", it) },
        )

        private const val TAG = "AutoLock"

        @Provides
        @Singleton
        fun provideVaultFilterRepository(@ApplicationContext context: Context): VaultFilterRepository =
            DataStoreVaultFilterRepository(context.vaultFilterStore)

        @Provides
        fun provideVaultSearch(groups: GroupRepository, entries: EntryRepository): VaultSearch =
            VaultSearch(groups, entries)
    }
}
