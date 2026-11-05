package ru.kino.dev.vault.data.entry

import ru.kino.dev.core.EntryFieldData
import ru.kino.dev.core.EntrySummaryData
import ru.kino.dev.core.NativeCore
import ru.kino.dev.vault.attachment.AttachmentsEdit
import ru.kino.dev.vault.attachment.NewAttachment
import ru.kino.dev.vault.data.attachment.toAttachment
import ru.kino.dev.vault.data.attachment.toData
import ru.kino.dev.vault.data.changes.VaultChangesImpl
import ru.kino.dev.vault.entry.EntryFailure
import ru.kino.dev.vault.entry.EntryField
import ru.kino.dev.vault.entry.EntryRecord
import ru.kino.dev.vault.entry.EntryRepository
import ru.kino.dev.vault.entry.EntrySearch
import ru.kino.dev.vault.entry.EntrySummary
import ru.kino.dev.vault.entry.EntryTimes
import ru.kino.dev.vault.group.EntryId
import ru.kino.dev.vault.group.GroupId

/** [EntryRepository] over the native core. Nothing is cached: the core holds the entries in memory already. */
internal class EntryRepositoryImpl(
    private val core: NativeCore,
    private val changes: VaultChangesImpl,
) : EntryRepository {

    override suspend fun entries(dbKey: String, groupId: GroupId): List<EntrySummary> = entryCall {
        core.entries(dbKey, groupId.value).map { it.toSummary() }
    }

    override suspend fun search(dbKey: String, term: String): EntrySearch = entryCall {
        val found = core.searchEntries(dbKey, term)
        EntrySearch(term = found.term, entries = found.entries.map { it.toSummary() })
    }

    override suspend fun read(dbKey: String, entryId: EntryId): EntryRecord = entryCall {
        val entry = core.entry(dbKey, entryId.value)
        EntryRecord(
            groupId = GroupId(entry.groupId),
            fields = entry.fields.map { it.toField() },
            times = EntryTimes(created = entry.createdAt, modified = entry.modifiedAt),
            attachments = entry.attachments.map { it.toAttachment() },
        )
    }

    override suspend fun create(
        dbKey: String,
        groupId: GroupId,
        fields: List<EntryField>,
        attachments: List<NewAttachment>,
    ): EntryId =
        entryCall(notFound = { EntryFailure.GroupNotFound(it) }) {
            EntryId(core.createEntry(dbKey, groupId.value, fields.map { it.toData() }, attachments.map { it.toData() }))
        }.also { changes.changed(dbKey) }

    override suspend fun update(
        dbKey: String,
        entryId: EntryId,
        fields: List<EntryField>,
        attachments: AttachmentsEdit?,
    ) = entryCall {
        core.updateEntry(dbKey, entryId.value, fields.map { it.toData() }, attachments?.toData())
    }.also { changes.changed(dbKey) }

    override suspend fun delete(dbKey: String, entryId: EntryId) = entryCall {
        core.deleteEntry(dbKey, entryId.value)
    }.also { changes.changed(dbKey) }

    // The core names neither which is missing - the entry, or the group it goes to; the group is the one
    // picked from a list that may be stale, the entry was just read
    override suspend fun move(dbKey: String, entryId: EntryId, groupId: GroupId) =
        entryCall(notFound = { EntryFailure.GroupNotFound(it) }) {
            core.moveEntry(dbKey, entryId.value, groupId.value)
        }.also { changes.changed(dbKey) }

    private fun EntryFieldData.toField() = EntryField(key = key, value = value, protected = protected, type = type)

    private fun EntryField.toData() = EntryFieldData(key = key, value = value, protected = protected, type = type)
}

/** An entry as a list shows it - also in the recycle bin. */
internal fun EntrySummaryData.toSummary() = EntrySummary(
    id = EntryId(id),
    groupId = GroupId(groupId),
    title = title,
    userName = userName,
    createdAt = createdAt,
)
