package ru.kino.dev.vault.pres.entryread

/**
 * What the screen gives [EntryReadProcessor] through Koin: one value with named fields, so that several ids of one type
 * cannot swap places on the way, as positional parameters could (pass-docs, Step 84).
 *
 * @param dbKey the open database
 * @param entryId the entry shown
 */
internal data class EntryReadArgs(
    val dbKey: String,
    val entryId: String,
)
