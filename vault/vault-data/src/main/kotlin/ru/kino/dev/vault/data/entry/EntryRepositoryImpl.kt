package ru.kino.dev.vault.data.entry

import ru.kino.dev.core.EntryFieldData
import ru.kino.dev.core.EntrySummaryData
import ru.kino.dev.core.NativeCore
import ru.kino.dev.vault.entry.EntryFailure
import ru.kino.dev.vault.entry.EntryField
import ru.kino.dev.vault.entry.EntryRecord
import ru.kino.dev.vault.entry.EntryRepository
import ru.kino.dev.vault.entry.EntrySearch
import ru.kino.dev.vault.entry.EntrySummary
import ru.kino.dev.vault.entry.EntryTimes
import ru.kino.dev.vault.group.EntryId
import ru.kino.dev.vault.group.GroupId
import javax.inject.Inject
import javax.inject.Singleton

/** [EntryRepository] over the native core. Nothing is cached: the core holds the entries in memory already. */
@Singleton
internal class EntryRepositoryImpl @Inject constructor(
    private val core: NativeCore,
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
        )
    }

    override suspend fun create(dbKey: String, groupId: GroupId, fields: List<EntryField>): EntryId =
        entryCall(notFound = { EntryFailure.GroupNotFound(it) }) {
            EntryId(core.createEntry(dbKey, groupId.value, fields.map { it.toData() }))
        }

    override suspend fun update(dbKey: String, entryId: EntryId, fields: List<EntryField>) = entryCall {
        core.updateEntry(dbKey, entryId.value, fields.map { it.toData() })
    }

    override suspend fun delete(dbKey: String, entryId: EntryId) = entryCall {
        core.deleteEntry(dbKey, entryId.value)
    }

    // The core names neither which is missing - the entry, or the group it goes to; the group is the one
    // picked from a list that may be stale, the entry was just read
    override suspend fun move(dbKey: String, entryId: EntryId, groupId: GroupId) =
        entryCall(notFound = { EntryFailure.GroupNotFound(it) }) {
            core.moveEntry(dbKey, entryId.value, groupId.value)
        }

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
