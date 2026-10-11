package ru.kino.dev.core

/**
 * The groups of an open database, as the core describes them: every group by id, the root and the recycle
 * bin.
 *
 * Ids are the core's uuids as strings. Entries are only named here by id - what they hold comes with the
 * calls of the entries themselves.
 */
data class GroupTree(
    val rootId: String,
    /** Null while the database has no recycle bin; the core creates one on the first delete */
    val recycleBinId: String?,
    val groups: Map<String, GroupNode>,
)

/** One group of a [GroupTree]: enough to draw it and walk the tree, not enough to edit it. */
data class GroupNode(
    val id: String,
    /** Null for the root */
    val parentId: String?,
    val name: String,
    /** The groups inside, in the order the database keeps them */
    val groupIds: List<String>,
    /** The entries directly inside, not those of the groups below */
    val entryIds: List<String>,
)
