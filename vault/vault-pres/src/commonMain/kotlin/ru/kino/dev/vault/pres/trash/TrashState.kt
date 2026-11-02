package ru.kino.dev.vault.pres.trash

import ru.kino.dev.vault.group.EntryId
import ru.kino.dev.vault.group.GroupId
import ru.kino.dev.vault.pres.common.GroupChoice
import ru.kino.dev.vault.trash.TrashEntry

/**
 * @param entries what the bin holds; null until read - a skeleton meanwhile
 * @param query the search field
 * @param searching the typing goes on, or the core looks - a skeleton meanwhile
 * @param found what the search found; null - no search, the whole bin
 * @param picked the cards chosen; empty - no choice going on
 * @param dialog the dialog shown, if any
 * @param message for the snackbar, until it takes it
 */
data class TrashState(
    val entries: List<TrashEntry>? = null,
    val query: String = "",
    val searching: Boolean = false,
    val found: List<TrashEntry>? = null,
    val picked: Set<EntryId> = emptySet(),
    val dialog: TrashDialog? = null,
    val message: TrashMessage? = null,
) {
    /** The cards: the last deleted first; deleted in the same second - by title, case aside. */
    val shown: List<TrashEntry> = (found ?: entries.orEmpty()).sortedWith(NEWEST_FIRST)

    val loading: Boolean = entries == null

    val choosing: Boolean = picked.isNotEmpty()

    /** Why there is no card; null while there are cards, or while something is read. */
    val empty: TrashEmpty? = when {
        loading || searching || shown.isNotEmpty() -> null
        found != null -> TrashEmpty.NothingFound
        else -> TrashEmpty.Bin
    }

    private companion object {
        val NEWEST_FIRST = compareByDescending<TrashEntry> { it.deletedAt }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.entry.title }
    }
}

/** The dialog over the bin, if any. */
sealed interface TrashDialog {

    /** The change is under way: the dialog shows progress and takes nothing. */
    val saving: Boolean

    /**
     * Where the picked entries go: one group for all of them.
     *
     * @param groups the groups to choose from, in the order of the tree; the bin is not among them
     * @param group the one chosen; null - none yet, "restore" waits
     */
    data class Restore(
        val groups: List<GroupChoice>,
        val group: GroupId?,
        override val saving: Boolean = false,
    ) : TrashDialog

    /** "Delete for good?" - the [count] entries picked. */
    data class Remove(val count: Int, override val saving: Boolean = false) : TrashDialog

    /** "Empty the recycle bin?" - all it holds. */
    data class Empty(override val saving: Boolean = false) : TrashDialog
}

/** The same dialog, its change under way. */
internal fun TrashDialog.saving(): TrashDialog = when (this) {
    is TrashDialog.Restore -> copy(saving = true)
    is TrashDialog.Remove -> copy(saving = true)
    is TrashDialog.Empty -> copy(saving = true)
}

/** Why the list is empty - what the empty screen says. */
enum class TrashEmpty {
    /** Nothing in the bin. */
    Bin,

    /** The search found nothing. */
    NothingFound,
}

/** What the snackbar says. */
enum class TrashMessage {
    /** The bin could not be read. */
    LoadFailed,

    /** The search failed. */
    SearchFailed,

    /** The change is made, but the file was not written: it waits for the next save. */
    NotSaved,

    /** The bin changed meanwhile - an entry gone, the group gone: nothing was done, the list is read again. */
    Changed,

    /** The core refused - the database closed meanwhile, or a bug. */
    Unexpected,
}
