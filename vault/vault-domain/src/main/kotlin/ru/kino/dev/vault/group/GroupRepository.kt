package ru.kino.dev.vault.group

/**
 * The groups of the open database.
 *
 * Every change is made in memory only - the file gets it when the database is saved. Changes go through
 * [GroupEditor], which checks them and saves; this is what it is built on.
 *
 * Throws [GroupFailure] and nothing else.
 */
interface GroupRepository {

    /** The whole tree, its root named as the database ([VaultTree]). */
    suspend fun tree(dbKey: String): VaultTree

    /** Creates a group called [name] inside [parentId] and returns its id. */
    suspend fun create(dbKey: String, parentId: GroupId, name: String): GroupId

    /** Renames the group; nothing else of it changes. */
    suspend fun rename(dbKey: String, groupId: GroupId, name: String)

    /**
     * Deletes the group with its subtree: the entries go to the recycle bin flat - or for good when the database does
     * not use one - and the groups go for good (pass-docs, Step 69).
     */
    suspend fun delete(dbKey: String, groupId: GroupId)
}
