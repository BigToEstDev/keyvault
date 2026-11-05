package ru.kino.dev.vault.data.history

import ru.kino.dev.core.NativeCore
import ru.kino.dev.vault.data.attachment.toAttachment
import ru.kino.dev.vault.data.changes.VaultChangesImpl
import ru.kino.dev.vault.data.entry.entryCall
import ru.kino.dev.vault.entry.EntryField
import ru.kino.dev.vault.entry.EntryRecord
import ru.kino.dev.vault.entry.EntryTimes
import ru.kino.dev.vault.group.EntryId
import ru.kino.dev.vault.group.GroupId
import ru.kino.dev.vault.history.EntryHistoryRepository
import ru.kino.dev.vault.history.HistoryVersion

/** [EntryHistoryRepository] over the native core; fails as the entry repository does - the history is the entry's. */
internal class EntryHistoryRepositoryImpl(
    private val core: NativeCore,
    private val changes: VaultChangesImpl,
) : EntryHistoryRepository {

    override suspend fun versions(dbKey: String, entryId: EntryId): List<HistoryVersion> = entryCall {
        core.entryHistory(dbKey, entryId.value).map { HistoryVersion(index = it.index, modified = it.modifiedAt) }
    }

    override suspend fun version(dbKey: String, entryId: EntryId, index: Int): EntryRecord = entryCall {
        val version = core.historyVersion(dbKey, entryId.value, index)
        EntryRecord(
            groupId = GroupId(version.groupId),
            fields = version.fields.map { EntryField(key = it.key, value = it.value, protected = it.protected, type = it.type) },
            times = EntryTimes(created = version.createdAt, modified = version.modifiedAt),
            attachments = version.attachments.map { it.toAttachment() },
        )
    }

    // `NotFound` names neither the entry nor the version: both are EntryNotFound - the screen reads the
    // history again and sees which
    override suspend fun restore(dbKey: String, entryId: EntryId, index: Int) = entryCall {
        core.restoreHistoryVersion(dbKey, entryId.value, index)
    }.also { changes.changed(dbKey) }

    override suspend fun delete(dbKey: String, entryId: EntryId, index: Int) = entryCall {
        core.deleteHistoryVersion(dbKey, entryId.value, index)
    }.also { changes.changed(dbKey) }

    override suspend fun deleteAll(dbKey: String, entryId: EntryId) = entryCall {
        core.deleteHistory(dbKey, entryId.value)
    }.also { changes.changed(dbKey) }
}
