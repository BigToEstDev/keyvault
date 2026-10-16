package ru.kino.dev.vault.pres.entryhistory

sealed interface EntryHistoryAction {

    /** The screen is shown - the first time, or again: the history is read anew. */
    data object Load : EntryHistoryAction

    data object Back : EntryHistoryAction

    /** "Restore this version" in the menu of a card: asks first. */
    data class Restore(val index: Int) : EntryHistoryAction

    /** "Delete version" in the menu of a card: asks first. */
    data class DeleteVersion(val index: Int) : EntryHistoryAction

    /** "Delete history" in the menu of the top bar: asks first. */
    data object DeleteHistory : EntryHistoryAction

    data object DialogConfirm : EntryHistoryAction

    data object DialogDismiss : EntryHistoryAction

    /** The snackbar took the message. */
    data object MessageShown : EntryHistoryAction
}

/** The dialog over the history, if any. */
sealed interface EntryHistoryDialog {

    /** The change is under way: the dialog shows progress and takes nothing. */
    val saving: Boolean

    /**
     * "Restore the version?" - the version at [index], saved at [modified], becomes the entry; the current
     * state stays in the history.
     */
    data class Restore(val index: Int, val modified: Long, override val saving: Boolean = false) : EntryHistoryDialog

    /** "Delete the version?" - the version at [index], saved at [modified]. */
    data class Delete(val index: Int, val modified: Long, override val saving: Boolean = false) : EntryHistoryDialog

    /** "Delete the history?" - every version; when the entry was made stays. */
    data class DeleteAll(override val saving: Boolean = false) : EntryHistoryDialog
}

/** What the snackbar says. */
enum class EntryHistoryMessage { LoadFailed, NotSaved, Unexpected }
