package ru.kino.dev.ffi

import ru.kino.dev.core.GroupNode
import ru.kino.dev.core.GroupTree

/**
 * The tree as the rest of the app reads it.
 *
 * The core spells "none" as the nil uuid - the parent of the root, the recycle bin of a database that has
 * none yet - and that stops here: above this module it is a null.
 */
internal fun GroupTreeDto.toDomain() = GroupTree(
    rootId = rootUuid,
    recycleBinId = recycleBinUuid.orNullIfNil(),
    groups = groups.values.associate { it.uuid to it.toDomain() },
)

private fun GroupSummaryDto.toDomain() = GroupNode(
    id = uuid,
    parentId = parentGroupUuid.orNullIfNil(),
    name = name,
    groupIds = groupUuids,
    entryIds = entryUuids,
)

/**
 * The same group under another name. Everything else goes back as read: the core takes the whole group on
 * `insert_group` and `update_group`, and a field left out here would be a field reset there.
 */
internal fun GroupDto.named(name: String) = copy(name = name)

private fun String.orNullIfNil(): String? = takeUnless { it == NIL_UUID }

private const val NIL_UUID = "00000000-0000-0000-0000-000000000000"
