package ru.kino.dev.vault.pres.grouplist

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

    data object CreateGroup : GroupListAction

    data object CreateEntry : GroupListAction

    data class RenameGroup(val id: GroupId) : GroupListAction

    data class DeleteGroup(val id: GroupId) : GroupListAction

    data class DialogNameChanged(val name: String) : GroupListAction

    data object DialogConfirm : GroupListAction

    data object DialogDismiss : GroupListAction

    /** The snackbar took the message. */
    data object MessageShown : GroupListAction

    /** Lock the database - for now only the debug button; automatic locking of `vault` comes later. */
    data object Lock : GroupListAction
}

/** A group as its card shows it. */
data class GroupItem(
    val id: GroupId,
    val name: String,
    /** Entries in it and in every group below it */
    val entryCount: Int,
)

/** One part of the path in the top bar. */
data class Crumb(val id: GroupId, val name: String)

/** The dialog over the list, if any. */
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
        override val saving: Boolean = false,
    ) : GroupDialog
}

/** What is wrong with a typed name - shown under the field. */
enum class NameProblem { Empty, TooLong, Taken }

/** What the snackbar says. */
enum class GroupListMessage { NotSaved, LoadFailed, Unexpected }
