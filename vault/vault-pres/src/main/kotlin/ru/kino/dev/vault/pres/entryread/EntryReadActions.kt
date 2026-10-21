package ru.kino.dev.vault.pres.entryread

sealed interface EntryReadAction {

    /** The screen is shown - the first time, or again after editing: the entry is read anew. */
    data object Load : EntryReadAction

    data object Back : EntryReadAction

    /** The fab with the pen: the form of this entry. */
    data object Edit : EntryReadAction

    data object TogglePassword : EntryReadAction

    /** Shows or hides the value of the protected own field at [index]. */
    data class ToggleOwn(val index: Int) : EntryReadAction

    /** Shows or hides the value of the field of the preset under [key], private by the database. */
    data class ToggleKey(val key: String) : EntryReadAction

    /** "History" in the menu of the top bar: the old versions of this entry. */
    data object History : EntryReadAction

    /** "Delete" in the menu of the top bar: asks first. */
    data object Delete : EntryReadAction

    /** "Delete" in the dialog: the entry goes into the recycle bin, the screen goes back. */
    data object DeleteConfirm : EntryReadAction

    data object DeleteDismiss : EntryReadAction

    /** Lock the database now, not waiting for the timer - the lock button (pass-docs, Step 59). */
    data object Lock : EntryReadAction

    /** The snackbar took the message. */
    data object MessageShown : EntryReadAction
}

/**
 * "Delete the entry?" - the entry called [title] as the file holds it.
 *
 * @param deleting the delete is under way: the dialog shows progress and takes nothing
 */
data class DeleteDialog(val title: String, val deleting: Boolean = false)

/** What the snackbar says. */
enum class EntryReadMessage { LoadFailed, NotSaved, Unexpected }
