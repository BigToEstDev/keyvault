package ru.kino.dev.vault.pres.entryhistory

import ru.kino.dev.vault.history.EntryHistory

/**
 * @param loading the history is not read yet - the skeleton is shown
 * @param history the name of the entry, its old versions newest first and when it was made; null until read
 * @param dialog asking about a version, if any
 * @param message for the snackbar, until it takes it
 */
data class EntryHistoryState(
    val loading: Boolean = true,
    val history: EntryHistory? = null,
    val dialog: EntryHistoryDialog? = null,
    val message: EntryHistoryMessage? = null,
)

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
