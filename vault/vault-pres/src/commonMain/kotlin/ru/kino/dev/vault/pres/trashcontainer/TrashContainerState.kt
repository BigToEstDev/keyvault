package ru.kino.dev.vault.pres.trashcontainer

/**
 * What the container of the recycle bin holds on a wide window (pass-docs, Step 100, item 2): the entries of the bin
 * open on the right, as tabs; the list of the bin on the left is always the same.
 *
 * @param tabs the entries open, in the order they were opened
 * @param active the entry shown; null - none open
 * @param tabLimit [TRASH_MAX_TABS] open, one more asked for: "close one first" is shown (pass-docs, Step 99, item 10)
 */
data class TrashContainerState(
    val tabs: List<TrashTab> = emptyList(),
    val active: String? = null,
    val tabLimit: Boolean = false,
) {
    /** The tab shown; null - none. */
    val shown: TrashTab? = tabs.firstOrNull { it.entryId == active }

    /** The panes to keep - the list and the tabs; the rest are cleared. */
    val paneKeys: Set<String> = setOf(TRASH_LIST_PANE) + tabs.map { trashEntryPane(it.entryId) }
}

/**
 * An entry of the bin open in a tab - read only, no pages over it. [title] - null while it is read, empty - the
 * entry has none.
 */
data class TrashTab(val entryId: String, val title: String? = null)

/** The most tabs the container of the bin keeps open: each holds an entry read in memory (Step 99, item 10). */
const val TRASH_MAX_TABS = 12

/** The key of the pane of the list of the bin. */
const val TRASH_LIST_PANE = "trash"

/** The key of the pane of the entry [entryId] of the bin. */
fun trashEntryPane(entryId: String): String = "trash:$entryId"
