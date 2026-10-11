package ru.kino.dev.vault.group

/**
 * [GroupRepository] in memory, for tests of what is built on it: the groups change the way the core changes
 * them, and every call is written to [calls].
 *
 * A failure is set per method in [failures] and thrown each time that method is called, after it is
 * recorded.
 */
class FakeGroupRepository(rootName: String = "Passwords") : GroupRepository {

    val calls = mutableListOf<String>()

    /** Method name to the failure it throws. */
    val failures = mutableMapOf<String, Exception>()

    val rootId = GroupId("root")

    var recycleBinId: GroupId? = null
        private set

    private val groups = LinkedHashMap<GroupId, VaultGroup>().apply {
        put(rootId, VaultGroup(rootId, null, rootName, emptyList(), emptyList()))
    }

    private var created = 0

    /** Puts a group with [entries] entries into [parentId], for setting a test up. Not recorded. */
    fun add(id: String, parentId: GroupId, name: String, entries: Int = 0): GroupId {
        val groupId = GroupId(id)
        groups[groupId] = VaultGroup(
            id = groupId,
            parentId = parentId,
            name = name,
            childGroupIds = emptyList(),
            entryIds = List(entries) { EntryId("$id-entry-$it") },
        )
        val parent = groups.getValue(parentId)
        groups[parentId] = parent.copy(childGroupIds = parent.childGroupIds + groupId)
        return groupId
    }

    /** Puts [count] more entries straight into [groupId] - the root among others. Not recorded. */
    fun addEntries(groupId: GroupId, count: Int) {
        val group = groups.getValue(groupId)
        val more = List(count) { EntryId("${groupId.value}-entry-${group.entryIds.size + it}") }
        groups[groupId] = group.copy(entryIds = group.entryIds + more)
    }

    fun name(id: GroupId): String? = groups[id]?.name

    override suspend fun tree(dbKey: String): VaultTree {
        call("tree")
        return VaultTree(rootId, recycleBinId, groups.toMap())
    }

    override suspend fun create(dbKey: String, parentId: GroupId, name: String): GroupId {
        call("create")
        if (parentId !in groups) throw GroupFailure.GroupNotFound()
        return add("group-${++created}", parentId, name)
    }

    override suspend fun rename(dbKey: String, groupId: GroupId, name: String) {
        call("rename")
        val group = groups[groupId] ?: throw GroupFailure.GroupNotFound()
        groups[groupId] = group.copy(name = name)
    }

    override suspend fun moveToRecycleBin(dbKey: String, groupId: GroupId) {
        call("moveToRecycleBin")
        val group = groups[groupId] ?: throw GroupFailure.GroupNotFound()
        val bin = recycleBinId ?: add("recycle-bin", rootId, "Recycle Bin").also { recycleBinId = it }

        val oldParent = groups.getValue(checkNotNull(group.parentId))
        groups[oldParent.id] = oldParent.copy(childGroupIds = oldParent.childGroupIds - groupId)
        val newParent = groups.getValue(bin)
        groups[bin] = newParent.copy(childGroupIds = newParent.childGroupIds + groupId)
        groups[groupId] = group.copy(parentId = bin)
    }

    private fun call(method: String) {
        calls += method
        failures[method]?.let { throw it }
    }
}
