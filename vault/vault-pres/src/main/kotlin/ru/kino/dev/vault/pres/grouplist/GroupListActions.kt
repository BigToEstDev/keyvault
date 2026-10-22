package ru.kino.dev.vault.pres.grouplist

import ru.kino.dev.vault.filter.CreationOrder
import ru.kino.dev.vault.filter.Visibility
import ru.kino.dev.vault.group.EntryCount
import ru.kino.dev.vault.group.EntryId
import ru.kino.dev.vault.group.GroupId

sealed interface GroupListAction {

    /** The screen is shown - the first time, or again after "back" or unlocking: the tree is read anew. */
    data object Load : GroupListAction

    data class QueryChanged(val query: String) : GroupListAction

    data class OpenGroup(val id: GroupId) : GroupListAction

    /** A part of the path in the top bar, counted from the root. */
    data class OpenCrumb(val index: Int) : GroupListAction

    data object Back : GroupListAction

    data object ToggleFab : GroupListAction

    /** Closes the menu of the fab - "back" or a tap past it; never opens it, unlike [ToggleFab]. */
    data object CloseFab : GroupListAction

    /** The filter button by the search field: the dialog opens with the filter in effect. */
    data object OpenFilters : GroupListAction

    /** The cross of the dialog, or a tap past it: what was chosen in it is dropped. */
    data object CloseFilters : GroupListAction

    data class FilterVisibilityChanged(val visibility: Visibility) : GroupListAction

    data class FilterOrderChanged(val order: CreationOrder) : GroupListAction

    /** What is chosen in the dialog takes effect and is kept on the device. */
    data object ApplyFilters : GroupListAction

    /** The default takes effect and is kept on the device, whatever is chosen in the dialog. */
    data object ResetFilters : GroupListAction

    data object CreateGroup : GroupListAction

    data object CreateEntry : GroupListAction

    /** The gear of the root: the settings (pass-docs, Step 52). */
    data object OpenSettings : GroupListAction

    /** The bin of the root, beside the gear: the recycle bin (pass-docs, Step 70). */
    data object OpenTrash : GroupListAction

    data class OpenEntry(val id: EntryId) : GroupListAction

    data class EditEntry(val id: EntryId) : GroupListAction

    data class DeleteEntry(val id: EntryId) : GroupListAction

    data class RenameGroup(val id: GroupId) : GroupListAction

    data class DeleteGroup(val id: GroupId) : GroupListAction

    /** "Edit" in the menu of the top bar: the group shown here, not one of its cards. */
    data object RenameCurrent : GroupListAction

    /** "Delete" in the menu of the top bar: the group shown here; deleted, the screen goes to its parent. */
    data object DeleteCurrent : GroupListAction

    data class DialogNameChanged(val name: String) : GroupListAction

    data object DialogConfirm : GroupListAction

    data object DialogDismiss : GroupListAction

    /** The snackbar took the message. */
    data object MessageShown : GroupListAction

    /** Lock the database now, not waiting for the timer - the lock button (pass-docs, Step 59). */
    data object Lock : GroupListAction
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
