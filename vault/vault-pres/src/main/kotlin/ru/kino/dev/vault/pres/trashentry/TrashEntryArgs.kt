package ru.kino.dev.vault.pres.trashentry

/**
 * What the screen gives [TrashEntryProcessor] through Koin: one value with named fields, so that several ids of one type
 * cannot swap places on the way, as positional parameters could (pass-docs, Step 84).
 *
 * @param dbKey the open database
 * @param entryId the entry in the trash shown
 */
internal data class TrashEntryArgs(
    val dbKey: String,
    val entryId: String,
)
