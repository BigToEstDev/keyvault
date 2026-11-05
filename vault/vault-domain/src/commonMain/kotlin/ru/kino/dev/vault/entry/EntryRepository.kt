package ru.kino.dev.vault.entry

import ru.kino.dev.vault.attachment.AttachmentsEdit
import ru.kino.dev.vault.attachment.EntryAttachment
import ru.kino.dev.vault.attachment.NewAttachment
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

    /**
     * Creates an entry inside [groupId] holding exactly [fields] and the files [attachments], in one call, and returns
     * its id. The bytes of the files stay the caller's to wipe.
     */
    suspend fun create(
        dbKey: String,
        groupId: GroupId,
        fields: List<EntryField>,
        attachments: List<NewAttachment> = emptyList(),
    ): EntryId

    /**
     * Replaces the fields of the entry with [fields] and - when [attachments] is given - its attachments with those
     * kept and added, in one call; null keeps them as they are. Nothing else of it changes. The bytes of the files
     * added stay the caller's to wipe.
     */
    suspend fun update(dbKey: String, entryId: EntryId, fields: List<EntryField>, attachments: AttachmentsEdit? = null)

    /** Deletes the entry: into the recycle bin, or for good when the database does not use one (pass-docs, Step 69). */
    suspend fun delete(dbKey: String, entryId: EntryId)

    /** Moves the entry into the group [groupId]; nothing else of it changes. */
    suspend fun move(dbKey: String, entryId: EntryId, groupId: GroupId)
}

/** An entry as the core holds it. */
data class EntryRecord(
    /** The group the entry is directly in */
    val groupId: GroupId,
    /** In the order of the file */
    val fields: List<EntryField>,
    val times: EntryTimes = EntryTimes(),
    /** In the order of the file - names and sizes, the bytes only on "Download" */
    val attachments: List<EntryAttachment> = emptyList(),
)

/**
 * When an entry was made and last changed, in seconds since the epoch, UTC; null - the time could not be
 * read. A save that changes the entry changes [modified]; one that changes nothing writes nothing.
 */
data class EntryTimes(val created: Long? = null, val modified: Long? = null)

/**
 * An entry as its screens show it ([EntryOpener]): where it is, what it holds laid over its preset, when it was
 * made and changed, and its attachments.
 */
data class OpenedEntry(
    val groupId: GroupId,
    val content: EntryContent,
    val times: EntryTimes = EntryTimes(),
    val attachments: List<EntryAttachment> = emptyList(),
)
