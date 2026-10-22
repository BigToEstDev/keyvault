package ru.kino.dev.vault.pres.trash

import ru.kino.dev.vault.group.EntryId
import ru.kino.dev.vault.group.GroupId
import ru.kino.dev.vault.pres.common.GroupChoice

sealed interface TrashAction {

    /** The screen is shown - first, and on every return: the bin is read again. */
    data object Load : TrashAction

    /** "Back" in the top bar, or the system one: leaves the choice first, then the screen. */
    data object Back : TrashAction

    /** The search field changed: searched after a pause in typing; blank - the whole bin again. */
    data class QueryChanged(val query: String) : TrashAction

    /** A long press: the choice begins with this card, or it is added to it - as a tap while choosing. */
    data class LongPress(val id: EntryId) : TrashAction

    /** A tap: while choosing, the card is picked or let go; otherwise nothing - an entry in the bin is not read. */
    data class Tap(val id: EntryId) : TrashAction

    /** The cross of the choice: nothing picked any more. */
    data object ClearChoice : TrashAction

    /** Every card shown is picked. */
    data object PickAll : TrashAction

    /** "Restore" under the choice: the group to restore to is asked. */
    data object Restore : TrashAction

    /** A group chosen in "restore". */
    data class GroupChosen(val id: GroupId) : TrashAction

    /** "Delete" under the choice: asked first - for good. */
    data object Remove : TrashAction

    /** "Empty the recycle bin" in the menu of the top bar: asked first - for good. */
    data object Empty : TrashAction

    /** The answer of the dialog shown. */
    data object DialogConfirm : TrashAction

    data object DialogDismiss : TrashAction

    /** The snackbar took the message. */
    data object MessageShown : TrashAction
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
