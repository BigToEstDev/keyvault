package ru.kino.dev.vault.data.group

import ru.kino.dev.core.GroupNode
import ru.kino.dev.core.GroupTree
import ru.kino.dev.core.NativeCore
import ru.kino.dev.vault.data.changes.VaultChangesImpl
import ru.kino.dev.vault.group.EntryId
import ru.kino.dev.vault.group.GroupId
import ru.kino.dev.vault.group.GroupRepository
import ru.kino.dev.vault.group.VaultGroup
import ru.kino.dev.vault.group.VaultTree

/** [GroupRepository] over the native core. Nothing is cached: the core holds the tree in memory already. */
internal class GroupRepositoryImpl(
    private val core: NativeCore,
    private val changes: VaultChangesImpl,
) : GroupRepository {

    // The root stands for the database wherever it is shown, so it is named as the database: `Meta/DatabaseName`, what
    // `Файл базы` renames - not the root group, which KeePass keeps apart (pass-docs, Step 56, item 6)
    override suspend fun tree(dbKey: String): VaultTree = groupCall {
        val databaseName = core.databaseFile(dbKey).name
        core.groupTree(dbKey).toVaultTree(databaseName)
    }

    override suspend fun create(dbKey: String, parentId: GroupId, name: String): GroupId = groupCall {
        GroupId(core.createGroup(dbKey, parentId.value, name))
    }.also { changes.changed(dbKey) }

    override suspend fun rename(dbKey: String, groupId: GroupId, name: String) = groupCall {
        core.renameGroup(dbKey, groupId.value, name)
    }.also { changes.changed(dbKey) }

    override suspend fun delete(dbKey: String, groupId: GroupId) = groupCall {
        core.deleteGroup(dbKey, groupId.value)
    }.also { changes.changed(dbKey) }

    /** The root named [databaseName]; a blank one - the root group's own, as before. */
    private fun GroupTree.toVaultTree(databaseName: String) = VaultTree(
        rootId = GroupId(rootId),
        recycleBinId = recycleBinId?.let(::GroupId),
        groups = groups.values.associate {
            GroupId(it.id) to it.toVaultGroup(if (it.id == rootId) databaseName.ifBlank { it.name } else it.name)
        },
    )

    private fun GroupNode.toVaultGroup(name: String) = VaultGroup(
        id = GroupId(id),
        parentId = parentId?.let(::GroupId),
        name = name,
        childGroupIds = groupIds.map(::GroupId),
        entryIds = entryIds.map(::EntryId),
    )
}
