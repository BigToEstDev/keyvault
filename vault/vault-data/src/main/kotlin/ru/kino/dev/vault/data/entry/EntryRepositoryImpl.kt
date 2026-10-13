package ru.kino.dev.vault.data.entry

import ru.kino.dev.core.EntryFieldData
import ru.kino.dev.core.EntrySummaryData
import ru.kino.dev.core.NativeCore
import ru.kino.dev.vault.entry.EntryFailure
import ru.kino.dev.vault.entry.EntryField
import ru.kino.dev.vault.entry.EntryRepository
import ru.kino.dev.vault.entry.EntrySummary
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

    override suspend fun fields(dbKey: String, entryId: EntryId): List<EntryField> = entryCall {
        core.entryFields(dbKey, entryId.value).map { it.toField() }
    }

    override suspend fun create(dbKey: String, groupId: GroupId, fields: List<EntryField>): EntryId =
        entryCall(notFound = { EntryFailure.GroupNotFound(it) }) {
            EntryId(core.createEntry(dbKey, groupId.value, fields.map { it.toData() }))
        }

    override suspend fun update(dbKey: String, entryId: EntryId, fields: List<EntryField>) = entryCall {
        core.updateEntry(dbKey, entryId.value, fields.map { it.toData() })
    }

    override suspend fun moveToRecycleBin(dbKey: String, entryId: EntryId) = entryCall {
        core.moveEntryToRecycleBin(dbKey, entryId.value)
    }

    private fun EntrySummaryData.toSummary() = EntrySummary(id = EntryId(id), title = title, userName = userName)

    private fun EntryFieldData.toField() = EntryField(key = key, value = value, protected = protected)

    private fun EntryField.toData() = EntryFieldData(key = key, value = value, protected = protected)
}
