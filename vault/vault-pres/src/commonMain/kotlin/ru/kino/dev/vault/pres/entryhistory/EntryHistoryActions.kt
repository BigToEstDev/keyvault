package ru.kino.dev.vault.pres.entryhistory

/** What the history of an entry is asked to do - done by [EntryHistoryProcessor] (pass-docs, `docs/app/mvi-arc.md`). */
interface EntryHistoryActions {

    /** The screen is shown - the first time, or again: the history is read anew. */
    fun onLoad()

    fun onBack()

    /** A tap on a card of a version: the entry as it was then (pass-docs, Step 51, item 6). */
    fun onOpenVersion(index: Int)

    /** "Restore this version" in the menu of a card: asks first. */
    fun onRestore(index: Int)

    /** "Delete version" in the menu of a card: asks first. */
    fun onDeleteVersion(index: Int)

    /** "Delete history" in the menu of the top bar: asks first. */
    fun onDeleteHistory()

    fun onDialogConfirm()

    fun onDialogDismiss()

    /** The snackbar took the message. */
    fun onMessageShown()
}
