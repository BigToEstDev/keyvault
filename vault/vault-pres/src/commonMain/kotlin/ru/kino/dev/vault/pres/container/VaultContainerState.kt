package ru.kino.dev.vault.pres.container

/**
 * What the container of `vault` holds on a wide window (pass-docs, Step 98, `docs/dev/navigation.md`): the groups the
 * list on the left went down, and the entries open on the right, as tabs.
 *
 * @param groups the way down from the root, the root first (null): the list shows the last; "back" goes one up
 * @param tabs the entries open, in the order they were opened
 * @param active the key of the tab shown; null - none open
 * @param tabLimit [VAULT_MAX_TABS] open, one more asked for: "close one first" is shown (pass-docs, Step 99, item 10)
 */
data class VaultContainerState(
    val groups: List<String?> = listOf(null),
    val tabs: List<EntryTab> = emptyList(),
    val active: String? = null,
    val tabLimit: Boolean = false,
) {
    /** The group the list shows; null - the root. */
    val group: String? = groups.last()

    /** The tab shown; null - none. */
    val shown: EntryTab? = tabs.firstOrNull { it.key == active }

    /**
     * The panes to keep - the groups on the way down and the tabs, every page on their way; the rest are cleared. Back
     * from a page, the one under it is there as it was.
     */
    val paneKeys: Set<String> = groups.map(::groupPane).toSet() + tabs.flatMap { it.panes }
}

/**
 * A tab on the right - one page shown at a time (pass-docs, Step 98, items 6, 7): an entry, read or a page over its
 * reading, or a new one made.
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
     * The entry [entryId]: read, under [pages] - the way from its reading, the last shown (item 7). [title] - null
     * while it is read, empty - the entry has none.
     */
    data class Entry(val entryId: String, val title: String? = null, val pages: List<EntryPage> = emptyList()) : EntryTab {
        override val key: String = entryId

        /** The page shown; null - the reading. */
        val top: EntryPage? = pages.lastOrNull()

        override val pane: String = top?.pane(entryId) ?: entryPane(entryId)
        override val panes: List<String> = listOf(entryPane(entryId)) + pages.map { it.pane(entryId) }
    }

    /** A new entry made in [groupId] - its form; [key] - the tab's own, the entry has no id yet. */
    data class New(override val key: String, val groupId: String) : EntryTab {
        override val pane: String = key
        override val panes: List<String> = listOf(key)
    }
}

/** A page over the reading of an entry, in its tab: as on a phone a screen over it. */
sealed interface EntryPage {

    /** The key of the pane of the page, of the entry [entryId]. */
    fun pane(entryId: String): String

    /** Its form. */
    data object Edit : EntryPage {
        override fun pane(entryId: String) = formPane(entryId)
    }

    /** Its history. */
    data object History : EntryPage {
        override fun pane(entryId: String) = historyPane(entryId)
    }

    /** One old version of it, at [index] in its history. */
    data class Version(val index: Int) : EntryPage {
        override fun pane(entryId: String) = versionPane(entryId, index)
    }
}

/** The most tabs the container of `vault` keeps open: each holds an entry read - its secrets - in memory (Step 99, item 10). */
const val VAULT_MAX_TABS = 12

/** The key of the pane of the group [groupId] - null for the root. */
fun groupPane(groupId: String?): String = "group:${groupId ?: "root"}"

/** The key of the pane of the entry [entryId], read. */
fun entryPane(entryId: String): String = "entry:$entryId"

/** The key of the pane of the form of the entry [entryId]. */
fun formPane(entryId: String): String = "form:$entryId"

/** The key of the pane of the history of the entry [entryId]. */
fun historyPane(entryId: String): String = "history:$entryId"

/** The key of the pane of the version [index] of the entry [entryId]. */
fun versionPane(entryId: String, index: Int): String = "version:$entryId:$index"

/** The key of the tab - and of its pane - of the [number]th new entry made in the container. */
fun newTab(number: Int): String = "new:$number"
