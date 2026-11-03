package ru.kino.dev.vault.pres.container

/**
 * What the container of `vault` holds on a wide window (pass-docs, Step 98, `docs/dev/navigation.md`): the groups the
 * list on the left went down, and the entries open on the right, as tabs.
 *
 * @param groups the way down from the root, the root first (null): the list shows the last; "back" goes one up
 * @param tabs the entries open, in the order they were opened
 * @param active the tab shown; null - none open
 */
data class VaultContainerState(
    val groups: List<String?> = listOf(null),
    val tabs: List<EntryTab> = emptyList(),
    val active: String? = null,
) {
    /** The group the list shows; null - the root. */
    val group: String? = groups.last()

    /** The panes to keep - the groups on the way down and the tabs; the rest are cleared. */
    val paneKeys: Set<String> = groups.map(::groupPane).toSet() + tabs.map { entryPane(it.entryId) }
}

/** An entry open on the right. */
data class EntryTab(val entryId: String)

/** The key of the pane of the group [groupId] - null for the root. */
fun groupPane(groupId: String?): String = "group:${groupId ?: "root"}"

/** The key of the pane of the entry [entryId]. */
fun entryPane(entryId: String): String = "entry:$entryId"
