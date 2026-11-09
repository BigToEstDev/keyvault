package ru.kino.dev.vault.pres.grouplist

import ru.kino.dev.vault.entry.EntrySummary
import ru.kino.dev.vault.filter.VaultFilter
import ru.kino.dev.vault.filter.Visibility
import ru.kino.dev.vault.filter.applyTo
import ru.kino.dev.vault.group.EntryCount
import ru.kino.dev.vault.group.EntryId
import ru.kino.dev.vault.group.GroupId

/**
 * @param isRoot the screen is the root of the database - known from the route, before the tree is read, so
 * the top bar of a group below it never shows the root's while it loads (pass-docs, Step 40, fix 13)
 * @param loading the tree and the entries are not read yet - the skeleton is shown
 * @param path from the root down to this group; empty until the tree is read
 * @param entryCount entries of this group - its own, and with those below; at the root - the whole database
 * @param listed what is directly in this group, before the filter
 * @param query typed into the search field; not blank - the list is what it finds
 * @param searching a search waits for the typing to stop, or for its answer - the skeleton is shown
 * @param found what [query] finds in this group and below it, before the filter; null with no search
 * @param filter the filter in effect - as kept on the device
 * @param filterDraft the filter dialog is shown, with what is chosen in it so far
 * @param savingFilter the filter chosen is being kept - the dialog waits
 * @param userNameHidden the database hides user names (pass-docs, Step 65): none in the list, whatever the
 * appearance says (pass-docs, Step 56, item 5)
 * @param message for the snackbar, until it takes it
 * @param unsavedChangedElsewhere the root only: a change waits in memory after "Cancel" - the file was written
 * elsewhere (pass-docs, Step 108)
 * @param unsavedDamaged the root only: a write the file did not hold - the file may be damaged, the change waits in
 * memory (pass-docs, Step 112)
 */
data class GroupListState(
    val isRoot: Boolean = true,
    val loading: Boolean = true,
    val path: List<Crumb> = emptyList(),
    val entryCount: EntryCount = EntryCount.None,
    val listed: ListSource = ListSource(),
    val query: String = "",
    val searching: Boolean = false,
    val found: ListSource? = null,
    val filter: VaultFilter = VaultFilter.DEFAULT,
    val filterDraft: VaultFilter? = null,
    val savingFilter: Boolean = false,
    val fabExpanded: Boolean = false,
    val dialog: GroupDialog? = null,
    val locking: Boolean = false,
    val userNameHidden: Boolean = false,
    val message: GroupListMessage? = null,
    val unsavedChangedElsewhere: Boolean = false,
    val unsavedDamaged: Boolean = false,
) {
    // Worked out once for every state, not on every read
    private val shown = (found ?: listed).let { filter.applyTo(it.groups, it.entries) }

    /** The groups the list shows - of this group, or found by the search - in the order of the database. */
    val groups: List<GroupItem> = shown.groups

    /** The entries the list shows, after the groups: by when they were created, as the filter says. */
    val entries: List<EntryItem> = shown.entries.map {
        EntryItem(it.id, it.title, if (userNameHidden) "" else it.userName, found?.locations?.get(it.groupId))
    }

    val isSearch: Boolean = query.isNotBlank()

    /** The group shown here, once the tree is read; null at the root - the database, not a group. */
    val current: Crumb? = if (isRoot) null else path.lastOrNull()

    val filtersOpen: Boolean = filterDraft != null

    /** Nothing in the list. */
    val isEmpty: Boolean = groups.isEmpty() && entries.isEmpty()

    /** Why the list is empty, which says what to show in its place; null when it is not. */
    val emptyReason: EmptyReason? = when {
        !isEmpty -> null
        isSearch -> EmptyReason.NothingFound
        listed.groups.isEmpty() && listed.entries.isEmpty() -> EmptyReason.EmptyGroup
        filter.visibility == Visibility.GroupsOnly -> EmptyReason.NoGroups
        else -> EmptyReason.NoEntries
    }
}

/**
 * What a list is made of, before the filter: groups as their cards show them, entries as the core gives them.
 *
 * @param locations of a search: where the entries lie, by the group they are in
 */
data class ListSource(
    val groups: List<GroupItem> = emptyList(),
    val entries: List<EntrySummary> = emptyList(),
    val locations: Map<GroupId, List<String>> = emptyMap(),
)

enum class EmptyReason {
    /** The group holds nothing */
    EmptyGroup,

    /** The search finds nothing - or nothing the filter lets through */
    NothingFound,

    /** The group holds entries only, and the filter shows groups only */
    NoGroups,

    /** The group holds groups only, and the filter shows entries only */
    NoEntries,
}

/** A group as its card shows it. */
data class GroupItem(
    val id: GroupId,
    val name: String,
    /** Entries in it and in every group below it */
    val entryCount: EntryCount,
    /** Found by a search: the names from the group searched in down to the one it is in. Null otherwise */
    val location: List<String>? = null,
)

/** An entry as its card shows it. */
data class EntryItem(
    val id: EntryId,
    /** Empty when the entry has none: the card shows its own "untitled" */
    val title: String,
    /** Empty when the entry has none - in a file of another app: the card has no line for it then */
    val userName: String = "",
    /** Found by a search: the names from the group searched in down to the one it is in. Null otherwise */
    val location: List<String>? = null,
)

/** One part of the path in the top bar. */
data class Crumb(val id: GroupId, val name: String)

/** The dialog over the list, if any - about a group or an entry of it. */
sealed interface GroupDialog {

    /** The change is under way: the dialog shows progress and takes nothing. */
    val saving: Boolean

    data class Create(
        val name: String = "",
        val problem: NameProblem? = null,
        override val saving: Boolean = false,
    ) : GroupDialog

    data class Rename(
        val groupId: GroupId,
        val name: String,
        val problem: NameProblem? = null,
        override val saving: Boolean = false,
    ) : GroupDialog

    data class Delete(
        val groupId: GroupId,
        val name: String,
        /** Its entries go to the recycle bin; false - everything goes for good (pass-docs, Step 70) */
        val toBin: Boolean,
        override val saving: Boolean = false,
    ) : GroupDialog

    data class DeleteEntry(
        val entryId: EntryId,
        /** As the card shows it; empty when the entry has none */
        val title: String,
        /** Into the recycle bin; false - for good (pass-docs, Step 70) */
        val toBin: Boolean,
        override val saving: Boolean = false,
    ) : GroupDialog
}

/** What is wrong with a typed name - shown under the field. */
enum class NameProblem { Empty, TooLong, Taken }

/** What the snackbar says. */
enum class GroupListMessage { NotSaved, LoadFailed, SearchFailed, FilterNotSaved, Unexpected }
