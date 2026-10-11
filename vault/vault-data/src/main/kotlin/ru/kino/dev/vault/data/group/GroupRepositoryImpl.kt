package ru.kino.dev.vault.data.group

import ru.kino.dev.core.GroupNode
import ru.kino.dev.core.GroupTree
import ru.kino.dev.core.NativeCore
import ru.kino.dev.vault.group.EntryId
import ru.kino.dev.vault.group.GroupId
import ru.kino.dev.vault.group.GroupRepository
import ru.kino.dev.vault.group.VaultGroup
import ru.kino.dev.vault.group.VaultTree
import javax.inject.Inject
import javax.inject.Singleton

/** [GroupRepository] over the native core. Nothing is cached: the core holds the tree in memory already. */
@Singleton
internal class GroupRepositoryImpl @Inject constructor(
    private val core: NativeCore,
) : GroupRepository {

    override suspend fun tree(dbKey: String): VaultTree = groupCall {
        core.groupTree(dbKey).toVaultTree()
    }

    override suspend fun create(dbKey: String, parentId: GroupId, name: String): GroupId = groupCall {
        GroupId(core.createGroup(dbKey, parentId.value, name))
    }

    override suspend fun rename(dbKey: String, groupId: GroupId, name: String) = groupCall {
        core.renameGroup(dbKey, groupId.value, name)
    }

    override suspend fun moveToRecycleBin(dbKey: String, groupId: GroupId) = groupCall {
        core.moveGroupToRecycleBin(dbKey, groupId.value)
    }

    private fun GroupTree.toVaultTree() = VaultTree(
        rootId = GroupId(rootId),
        recycleBinId = recycleBinId?.let(::GroupId),
        groups = groups.values.associate { GroupId(it.id) to it.toVaultGroup() },
    )

    private fun GroupNode.toVaultGroup() = VaultGroup(
        id = GroupId(id),
        parentId = parentId?.let(::GroupId),
        name = name,
        childGroupIds = groupIds.map(::GroupId),
        entryIds = entryIds.map(::EntryId),
    )
}
