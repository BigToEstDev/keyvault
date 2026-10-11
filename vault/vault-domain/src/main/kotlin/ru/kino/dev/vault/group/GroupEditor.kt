package ru.kino.dev.vault.group

import ru.kino.dev.vault.VaultDatabaseRepository
import kotlin.coroutines.cancellation.CancellationException

/**
 * Creating, renaming and deleting groups, the way a person does it: one action, one write of the file.
 *
 * Each change is checked against a fresh tree, made in the core and saved - "Save" in the group dialog or
 * "Delete" in the confirmation is the explicit save (pass-docs, Step 36, decision 3). The checks live here
 * and not in the dialog alone: a screen can be wrong, the database cannot.
 *
 * Throws [GroupFailure] and nothing else.
 */
class GroupEditor(
    private val groups: GroupRepository,
    private val database: VaultDatabaseRepository,
) {

    /** Creates a group called [name] inside [parentId], saves, and returns its id. */
    suspend fun create(dbKey: String, parentId: GroupId, name: String): GroupId {
        val tree = groups.tree(dbKey)
        tree.group(parentId) ?: throw GroupFailure.GroupNotFound()
        GroupNameValidator.problem(tree, parentId, name)?.let { throw it }

        val id = groups.create(dbKey, parentId, GroupNameValidator.normalize(name))
        save(dbKey)
        return id
    }

    /** Renames the group and saves. The same name again is not a change: nothing is written. */
    suspend fun rename(dbKey: String, groupId: GroupId, name: String) {
        val tree = groups.tree(dbKey)
        val group = editable(tree, groupId)
        val normalized = GroupNameValidator.normalize(name)
        if (normalized == group.name) return
        GroupNameValidator.problem(tree, checkNotNull(group.parentId), normalized, renamedId = groupId)
            ?.let { throw it }

        groups.rename(dbKey, groupId, normalized)
        save(dbKey)
    }

    /** Moves the group, with everything inside it, into the recycle bin and saves. */
    suspend fun delete(dbKey: String, groupId: GroupId) {
        editable(groups.tree(dbKey), groupId)

        groups.moveToRecycleBin(dbKey, groupId)
        save(dbKey)
    }

    /**
     * The group, if a person may change it. The root is the database itself and is not offered by any
     * screen, so asking for it is a bug, not a state.
     */
    private fun editable(tree: VaultTree, groupId: GroupId): VaultGroup {
        if (groupId == tree.rootId) {
            throw GroupFailure.Unexpected(IllegalArgumentException("the root group is not changed by hand"))
        }
        return tree.group(groupId) ?: throw GroupFailure.GroupNotFound()
    }

    private suspend fun save(dbKey: String) {
        try {
            database.save(dbKey)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw GroupFailure.NotSaved(e)
        }
    }
}
