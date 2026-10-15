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

    /** The entry as the core holds it: the group it is in, its fields in the order of the file. */
    suspend fun read(dbKey: String, entryId: EntryId): EntryRecord

    /** Creates an entry inside [groupId] holding exactly [fields] and returns its id. */
    suspend fun create(dbKey: String, groupId: GroupId, fields: List<EntryField>): EntryId

    /** Replaces the fields of the entry with [fields]; nothing else of it changes. */
    suspend fun update(dbKey: String, entryId: EntryId, fields: List<EntryField>)

    /** Moves the entry into the recycle bin. */
    suspend fun moveToRecycleBin(dbKey: String, entryId: EntryId)

    /** Moves the entry into the group [groupId]; nothing else of it changes. */
    suspend fun move(dbKey: String, entryId: EntryId, groupId: GroupId)

    /** The entry as its screens show it: its group, and its fields laid over the standard preset. */
    suspend fun opened(dbKey: String, entryId: EntryId): OpenedEntry {
        val record = read(dbKey, entryId)
        return OpenedEntry(record.groupId, StandardPreset.preset.layout(record.fields))
    }

    /** The fields of the entry as its screens show them, laid over the standard preset. */
    suspend fun content(dbKey: String, entryId: EntryId): EntryContent = opened(dbKey, entryId).content
}

/** An entry as the core holds it. */
data class EntryRecord(
    /** The group the entry is directly in */
    val groupId: GroupId,
    /** In the order of the file */
    val fields: List<EntryField>,
)

/** An entry as its screens show it: where it is, and what it holds laid over its preset. */
data class OpenedEntry(val groupId: GroupId, val content: EntryContent)
