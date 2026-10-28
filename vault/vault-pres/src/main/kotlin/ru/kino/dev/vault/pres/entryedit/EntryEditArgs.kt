package ru.kino.dev.vault.pres.entryedit

/**
 * What the screen gives [EntryEditProcessor] through Koin: one value with named fields, so that several ids of one type
 * cannot swap places on the way, as positional parameters could (pass-docs, Step 84).
 *
 * @param dbKey the open database
 * @param groupId where a new entry goes; null when editing
 * @param entryId the entry edited; null when creating
 */
internal data class EntryEditArgs(
    val dbKey: String,
    val groupId: String?,
    val entryId: String?,
)
