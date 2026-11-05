package ru.kino.dev.vault.pres.entryread

import ru.kino.dev.vault.pres.common.EntryReadFieldsActions

/** What the reading of an entry is asked to do - done by [EntryReadProcessor] (pass-docs, `docs/app/mvi-arc.md`). */
interface EntryReadActions : EntryReadFieldsActions {

    /** The screen is shown - the first time, or again after editing: the entry is read anew. */
    fun onLoad()

    fun onBack()

    /** The fab with the pen: the form of this entry. */
    fun onEdit()

    /** "History" in the menu of the top bar: the old versions of this entry. */
    fun onHistory()

    /** "Delete" in the menu of the top bar: asks first. */
    fun onDelete()

    /** "Delete" in the dialog: the entry goes into the recycle bin, or for good; the screen goes back. */
    fun onDeleteConfirm()

    fun onDeleteDismiss()

    /** The file to save the attachment asked for as, picked - [uri]; it is written there. */
    fun onDownloadPicked(uri: String)

    /** Lock the database now, not waiting for the timer - the lock button (pass-docs, Step 59). */
    fun onLock()

    /** The snackbar took the message. */
    fun onMessageShown()
}
