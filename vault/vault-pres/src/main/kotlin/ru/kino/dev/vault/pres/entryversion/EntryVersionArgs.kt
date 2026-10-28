package ru.kino.dev.vault.pres.entryversion

/**
 * What the screen gives [EntryVersionProcessor] through Koin: one value with named fields, so that several ids of one type
 * cannot swap places on the way, as positional parameters could (pass-docs, Step 84).
 *
 * @param dbKey the open database
 * @param entryId the entry the version is of
 * @param index the version in the history of the entry
 */
internal data class EntryVersionArgs(
    val dbKey: String,
    val entryId: String,
    val index: Int,
)
