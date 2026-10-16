package ru.kino.dev.vault.data.history

import ru.kino.dev.core.NativeCore
import ru.kino.dev.vault.data.entry.entryCall
import ru.kino.dev.vault.group.EntryId
import ru.kino.dev.vault.history.EntryHistoryRepository
import ru.kino.dev.vault.history.HistoryVersion
import javax.inject.Inject
import javax.inject.Singleton

/** [EntryHistoryRepository] over the native core; fails as the entry repository does - the history is the entry's. */
@Singleton
internal class EntryHistoryRepositoryImpl @Inject constructor(
    private val core: NativeCore,
) : EntryHistoryRepository {

    override suspend fun versions(dbKey: String, entryId: EntryId): List<HistoryVersion> = entryCall {
        core.entryHistory(dbKey, entryId.value).map { HistoryVersion(index = it.index, modified = it.modifiedAt) }
    }

    // `NotFound` names neither the entry nor the version: both are EntryNotFound - the screen reads the
    // history again and sees which
    override suspend fun restore(dbKey: String, entryId: EntryId, index: Int) = entryCall {
        core.restoreHistoryVersion(dbKey, entryId.value, index)
    }

    override suspend fun delete(dbKey: String, entryId: EntryId, index: Int) = entryCall {
        core.deleteHistoryVersion(dbKey, entryId.value, index)
    }
}
