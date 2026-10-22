package ru.kino.dev.vault.group

/** One group, as the tree holds it. */
data class VaultGroup(
    val id: GroupId,
    /** Null for the root */
    val parentId: GroupId?,
    val name: String,
    /** The groups inside, in the order the database keeps them */
    val childGroupIds: List<GroupId>,
    /** The entries directly inside, not those of the groups below */
    val entryIds: List<EntryId>,
)

/**
 * The groups of the open database, as `vault` shows them.
 *
 * What is visible is what can be reached from the root without entering the recycle bin: the bin is not
 * shown in v1, its groups cannot be opened, and its entries are not counted (pass-docs, Step 36, decision
 * 2). Every question about a group that is not visible answers as if there were no such group.
 *
 * The root is named as the database - `Meta/DatabaseName`, what the settings rename; a blank one - the root
 * group's own (pass-docs, Step 56, item 6). Wherever the root is shown it stands for the database.
 *
 * A snapshot: a change made through the repository is seen in the next tree, not in this one.
 */
class VaultTree(
    val rootId: GroupId,
    /** Null while the database has no recycle bin */
    val recycleBinId: GroupId?,
    private val groups: Map<GroupId, VaultGroup>,
) {

    init {
        require(rootId in groups) { "the root ${rootId.value} is not among the groups" }
    }

    val root: VaultGroup get() = groups.getValue(rootId)

    /**
     * Visible groups, walked once from the root. A visited set rather than trust in the shape: a file
     * damaged into a cycle must not hang the screen.
     */
    private val visible: Set<GroupId> by lazy {
        val seen = LinkedHashSet<GroupId>()
        val queue = ArrayDeque(listOf(rootId))
        while (queue.isNotEmpty()) {
            val id = queue.removeFirst()
            if (id == recycleBinId || !seen.add(id)) continue
            groups[id]?.childGroupIds?.let(queue::addAll)
        }
        seen
    }

    /** Entries of every visible group, its visible groups below included - counted once for the tree. */
    private val nestedCounts: Map<GroupId, Int> by lazy {
        val counts = HashMap<GroupId, Int>()
        // Reverse of the walk from the root: every group comes after the groups inside it
        for (id in visible.reversed()) {
            val group = groups.getValue(id)
            counts[id] = group.entryIds.size + group.childGroupIds.sumOf { counts[it] ?: 0 }
        }
        counts
    }

    /** The group, or null when there is none or it is not visible. */
    fun group(id: GroupId): VaultGroup? = if (id in visible) groups[id] else null

    /** The visible groups directly inside [id], in the order the database keeps them. */
    fun subgroups(id: GroupId): List<VaultGroup> =
        group(id)?.childGroupIds.orEmpty().mapNotNull(::group)

    /**
     * Every visible group below [id], at any depth - each right after the group it is in, siblings in the
     * order the database keeps them; [id] itself is not among them. Empty when [id] is not visible.
     */
    fun descendants(id: GroupId): List<VaultGroup> {
        val found = mutableListOf<VaultGroup>()
        // A visited set rather than trust in the shape, as for the walk from the root
        val seen = mutableSetOf(id)
        fun walk(parent: GroupId) {
            for (child in subgroups(parent)) {
                if (!seen.add(child.id)) continue
                found += child
                walk(child.id)
            }
        }
        if (group(id) != null) walk(id)
        return found
    }

    /** Whether [id] is [scopeId] or a visible group anywhere below it. */
    fun isWithin(id: GroupId, scopeId: GroupId): Boolean = path(id).any { it.id == scopeId }

    /**
     * The entries of [id]: right in it, and in it and every visible group below it. For the root the second is
     * every entry of the database outside the recycle bin. [EntryCount.None] when [id] is not visible.
     */
    fun entryCount(id: GroupId): EntryCount =
        group(id)?.let { EntryCount(direct = it.entryIds.size, nested = nestedCounts[id] ?: 0) } ?: EntryCount.None

    /** The groups from the root down to [id], both ends included; empty when [id] is not visible. */
    fun path(id: GroupId): List<VaultGroup> {
        if (group(id) == null) return emptyList()
        // Parents named in a damaged file may loop where the children lists do not: no path is longer than
        // the number of groups
        return generateSequence(group(id)) { it.parentId?.let(::group) }
            .take(visible.size)
            .toList()
            .asReversed()
    }

    /**
     * The groups from [scopeId] down to [id], both ends included - where [id] lies, seen from [scopeId].
     * Empty when [id] is not [scopeId] or a visible group below it.
     */
    fun pathFrom(scopeId: GroupId, id: GroupId): List<VaultGroup> {
        val full = path(id)
        val start = full.indexOfFirst { it.id == scopeId }
        return if (start < 0) emptyList() else full.drop(start)
    }

    /**
     * Whether a visible group inside [parentId] is already called [name], ignoring case and the spaces
     * around it. [renamedId] is the group being renamed: its own name is not a rival.
     */
    fun isNameTaken(parentId: GroupId, name: String, renamedId: GroupId? = null): Boolean {
        val wanted = name.trim()
        return subgroups(parentId).any { it.id != renamedId && it.name.trim().equals(wanted, ignoreCase = true) }
    }
}
