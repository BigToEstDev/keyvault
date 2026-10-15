package ru.kino.dev.vault.data

import android.content.Context
import android.os.SystemClock
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
import ru.kino.dev.vault.VaultDatabaseRepository
import ru.kino.dev.vault.data.entry.EntryRepositoryImpl
import ru.kino.dev.vault.data.filter.DataStoreVaultFilterRepository
import ru.kino.dev.vault.data.filter.vaultFilterStore
import ru.kino.dev.vault.data.group.GroupRepositoryImpl
import ru.kino.dev.vault.entry.EntryEditor
import ru.kino.dev.vault.entry.EntryRepository
import ru.kino.dev.vault.filter.VaultFilterRepository
import ru.kino.dev.vault.group.GroupEditor
import ru.kino.dev.vault.group.GroupRepository
import ru.kino.dev.vault.lock.AutoLock
import ru.kino.dev.vault.lock.DatabaseGuard
import ru.kino.dev.vault.search.VaultSearch
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

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

    companion object {

        // vault-domain is plain Kotlin and carries no injection annotations, so its use cases are built here
        @Provides
        fun provideGroupEditor(
            groups: GroupRepository,
            database: VaultDatabaseRepository,
            guard: DatabaseGuard,
        ): GroupEditor = GroupEditor(groups, database, guard)

        @Provides
        fun provideEntryEditor(
            entries: EntryRepository,
            database: VaultDatabaseRepository,
            guard: DatabaseGuard,
        ): EntryEditor = EntryEditor(entries, database, guard)

        // One for the app: the editors and the lock wait for each other on it
        @Provides
        @Singleton
        fun provideDatabaseGuard(): DatabaseGuard = DatabaseGuard()

        @Provides
        @Singleton
        fun provideAutoLock(database: VaultDatabaseRepository, guard: DatabaseGuard): AutoLock = AutoLock(
            database = database,
            guard = guard,
            // Counts while the phone sleeps, unlike the clock coroutines wait by
            clock = { SystemClock.elapsedRealtime().milliseconds },
            // The app's own: the lock outlives any screen
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
            timeout = AUTO_LOCK_TIMEOUT,
            onFailure = { Log.e(TAG, "the core refused to lock the database", it) },
        )

        /**
         * No touch this long, and the database locks - 5 minutes, the owner's (pass-docs, Step 46).
         * TODO(Step 46): 30 seconds to try it on the device; back to 5 minutes before the merge into main
         */
        private val AUTO_LOCK_TIMEOUT = 30.seconds

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
