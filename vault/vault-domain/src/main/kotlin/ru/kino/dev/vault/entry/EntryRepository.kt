package ru.kino.dev.vault.entry

import ru.kino.dev.vault.group.EntryId
import ru.kino.dev.vault.group.GroupId

/**
 * The entries of the open database.
 *
 * Every change is made in memory only - the file gets it when the database is saved. Changes go through
 * [EntryEditor], which checks them and saves; this is what it is built on.
 *
 * Throws [EntryFailure] and nothing else.
 */
interface EntryRepository {

    /** The entries directly inside [groupId], in the order the core gives them - by title. */
    suspend fun entries(dbKey: String, groupId: GroupId): List<EntrySummary>

    /**
     * The entries of the whole database found by [term]: by the title or the user name, as plain text,
     * case aside; never by a protected field, never in the recycle bin. A blank term finds nothing.
     */
    suspend fun search(dbKey: String, term: String): EntrySearch

    /** The fields of the entry, as the core holds them - in the order of the file. */
    suspend fun fields(dbKey: String, entryId: EntryId): List<EntryField>

    /** Creates an entry inside [groupId] holding exactly [fields] and returns its id. */
    suspend fun create(dbKey: String, groupId: GroupId, fields: List<EntryField>): EntryId

    /** Replaces the fields of the entry with [fields]; nothing else of it changes. */
    suspend fun update(dbKey: String, entryId: EntryId, fields: List<EntryField>)

    /** Moves the entry into the recycle bin. */
    suspend fun moveToRecycleBin(dbKey: String, entryId: EntryId)

    /** The entry as its screens show it: its fields laid over the standard preset. */
    suspend fun content(dbKey: String, entryId: EntryId): EntryContent =
        StandardPreset.preset.layout(fields(dbKey, entryId))
}
