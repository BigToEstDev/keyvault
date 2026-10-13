package ru.kino.dev.vault.data

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.kino.dev.vault.VaultDatabaseRepository
import ru.kino.dev.vault.data.entry.EntryRepositoryImpl
import ru.kino.dev.vault.data.group.GroupRepositoryImpl
import ru.kino.dev.vault.entry.EntryEditor
import ru.kino.dev.vault.entry.EntryRepository
import ru.kino.dev.vault.group.GroupEditor
import ru.kino.dev.vault.group.GroupRepository

/** Binds the repositories of `vault` to their implementations. */
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
        fun provideGroupEditor(groups: GroupRepository, database: VaultDatabaseRepository): GroupEditor =
            GroupEditor(groups, database)

        @Provides
        fun provideEntryEditor(entries: EntryRepository, database: VaultDatabaseRepository): EntryEditor =
            EntryEditor(entries, database)
    }
}
