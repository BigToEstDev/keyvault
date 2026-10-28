package ru.kino.dev.vault.trash

import ru.kino.dev.vault.entry.EntrySummary
import ru.kino.dev.vault.group.EntryId
import ru.kino.dev.vault.group.GroupId

/**
 * The recycle bin of the open database (pass-docs, Step 69 / 70): only entries go there - a deleted group sends the
 * entries of its subtree there and goes for good. Of `vault`: the database is open, and its file is written by
 * `vault`. Changes go through [TrashEditor], which writes the file.
 *
 * Throws [TrashFailure] and nothing else.
 */
interface TrashRepository {

    /** Whether the database uses the bin - `Meta/RecycleBinEnabled`; off, a delete is for good. */
    suspend fun enabled(dbKey: String): Boolean

    /** Switches the bin on or off - in memory only. Off, the bin goes at once with all it holds. */
    suspend fun setEnabled(dbKey: String, enabled: Boolean)

    /** The entries of the bin, from any depth of it, in no particular order - the screen sorts them. */
    suspend fun entries(dbKey: String): List<TrashEntry>

    /** The entries of the bin found by [term] - as the search of `vault`: the title or the user name. */
    suspend fun search(dbKey: String, term: String): TrashSearch

    /** Moves [entryIds] out of the bin into [groupId] - in memory only. All or nothing. */
    suspend fun restore(dbKey: String, entryIds: List<EntryId>, groupId: GroupId)

    /** Removes [entryIds] of the bin for good - in memory only. All or nothing. */
    suspend fun remove(dbKey: String, entryIds: List<EntryId>)

    /** Removes everything in the bin for good - in memory only. */
    suspend fun empty(dbKey: String)
}

/**
 * An entry of the bin.
 *
 * @param previousGroupId the group it was deleted from while that group lives - what a restore offers first
 * @param deletedAt when it went to the bin, in seconds since the epoch, UTC
 */
data class TrashEntry(
    val entry: EntrySummary,
    val previousGroupId: GroupId?,
    val deletedAt: Long,
)

/** The entries of the bin a term found, with the term: an answer to a term typed past is dropped. */
data class TrashSearch(val term: String, val entries: List<TrashEntry>)

/**
 * Whether a delete now goes to the bin - what the "delete?" dialog says. Unread, it says "for good": the safe side
 * to warn on.
 */
suspend fun TrashRepository.deletesToBin(dbKey: String): Boolean = try {
    enabled(dbKey)
} catch (e: TrashFailure) {
    false
}

/** Every way reading or changing the bin can fail. */
sealed class TrashFailure(message: String, cause: Throwable? = null) : Exception(message, cause) {

    /** The change is made in the database, but writing the file failed: it waits in memory for the next save. */
    class NotSaved(cause: Throwable? = null) : TrashFailure("The change is made but not written to the file", cause)

    /**
     * An entry is not in the bin any more, or the group to restore to is gone - the list was stale. Nothing changed;
     * the screen reads the bin again.
     */
    class Stale(cause: Throwable? = null) : TrashFailure("The bin has changed meanwhile", cause)

    /** The core refused - the database closed meanwhile, or a bug. The cause to the log. */
    class Unexpected(cause: Throwable? = null) : TrashFailure("Something went wrong", cause)
}
