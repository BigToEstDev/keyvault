package ru.kino.dev.vault.pres.grouplist

/**
 * What the screen gives [GroupListProcessor] through Koin: one value with named fields, so that several ids of one type
 * cannot swap places on the way, as positional parameters could (pass-docs, Step 84).
 *
 * @param dbKey the open database
 * @param groupId the group shown; null for the root
 */
internal data class GroupListArgs(
    val dbKey: String,
    val groupId: String?,
)
