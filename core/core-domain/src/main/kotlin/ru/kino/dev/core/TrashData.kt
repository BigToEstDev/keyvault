package ru.kino.dev.core

/**
 * An entry of the recycle bin (pass-docs, Step 69): only entries go there - a deleted group sends the entries of
 * its subtree there and goes for good.
 */
data class TrashEntryData(
    val entry: EntrySummaryData,
    /** The group the entry was deleted from while it lives - not removed, not in the bin; null when it is gone */
    val previousGroupId: String?,
    /** When the entry went to the bin, in seconds since the epoch, UTC */
    val deletedAt: Long,
)

/** The entries of the recycle bin a term finds, with the term itself - as [EntrySearchData]. */
data class TrashSearchData(
    val term: String,
    val entries: List<TrashEntryData>,
)
