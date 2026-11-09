package ru.kino.dev.vault.pres.sync

/** What the sync of `vault` is asked from its dialogs (pass-docs, Step 108). */
interface SyncActions {

    /** "Synchronize" in the dialog of a save that found the file written elsewhere. */
    fun onSynchronize()

    /** "Cancel" there: the change stays in memory, the main screen says so. */
    fun onCancel()

    /** "OK" in the dialog of what came in, or of what failed. */
    fun onDismiss()

    /** "Save" in the dialog of a write the file did not hold (pass-docs, Step 112). */
    fun onSave()

    /** "Later" there: the dialog closes, the main screen says so with a bar of its own. */
    fun onLater()
}
