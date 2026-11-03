package ru.kino.dev.vault.pres.container

/**
 * What the container of `vault` holds on a wide window (pass-docs, Step 98, `docs/dev/navigation.md`): the groups the
 * list on the left went down, and the entries open on the right, as tabs.
 *
 * @param groups the way down from the root, the root first (null): the list shows the last; "back" goes one up
 * @param tabs the entries open, in the order they were opened
 * @param active the key of the tab shown; null - none open
 */
data class VaultContainerState(
    val groups: List<String?> = listOf(null),
    val tabs: List<EntryTab> = emptyList(),
    val active: String? = null,
) {
    /** The group the list shows; null - the root. */
    val group: String? = groups.last()

    /** The tab shown; null - none. */
    val shown: EntryTab? = tabs.firstOrNull { it.key == active }

    /**
     * The panes to keep - the groups on the way down and the tabs; the rest are cleared. An entry edited keeps its
     * reading: back from the form, it is there as it was.
     */
    val paneKeys: Set<String> = groups.map(::groupPane).toSet() + tabs.flatMap { it.panes }
}

/**
 * A tab on the right - one page at a time (pass-docs, Step 98, item 6): an entry read or edited, or a new one made.
 *
 * @property key which tab: the id of its entry, or of the new one being made
 */
sealed interface EntryTab {
    val key: String

    /** The pane shown in the tab. */
    val pane: String

    /** The panes of the tab to keep. */
    val panes: List<String>

    /**
     * The entry [entryId]: read, or - [editing] - its form. [title] - null while it is read, empty - the entry has
     * none.
     */
    data class Entry(val entryId: String, val title: String? = null, val editing: Boolean = false) : EntryTab {
        override val key: String = entryId
        override val pane: String = if (editing) formPane(entryId) else entryPane(entryId)
        override val panes: List<String> = listOfNotNull(entryPane(entryId), formPane(entryId).takeIf { editing })
    }

    /** A new entry made in [groupId] - its form; [key] - the tab's own, the entry has no id yet. */
    data class New(override val key: String, val groupId: String) : EntryTab {
        override val pane: String = key
        override val panes: List<String> = listOf(key)
    }
}

/** The key of the pane of the group [groupId] - null for the root. */
fun groupPane(groupId: String?): String = "group:${groupId ?: "root"}"

/** The key of the pane of the entry [entryId], read. */
fun entryPane(entryId: String): String = "entry:$entryId"

/** The key of the pane of the form of the entry [entryId]. */
fun formPane(entryId: String): String = "form:$entryId"

/** The key of the tab - and of its pane - of the [number]th new entry made in the container. */
fun newTab(number: Int): String = "new:$number"
