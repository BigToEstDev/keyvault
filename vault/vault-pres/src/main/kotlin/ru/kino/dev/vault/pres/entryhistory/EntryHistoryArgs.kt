package ru.kino.dev.vault.pres.entryhistory

/**
 * What the screen gives [EntryHistoryProcessor] through Koin: one value with named fields, so that several ids of one type
 * cannot swap places on the way, as positional parameters could (pass-docs, Step 84).
 *
 * @param dbKey the open database
 * @param entryId the entry whose versions are shown
 */
internal data class EntryHistoryArgs(
    val dbKey: String,
    val entryId: String,
)
