package ru.kino.dev.vault.pres.entryversion

sealed interface EntryVersionAction {

    /** The screen is shown: the version is read. */
    data object Load : EntryVersionAction

    data object Back : EntryVersionAction

    data object TogglePassword : EntryVersionAction

    /** Shows or hides the value of the protected own field at [index]. */
    data class ToggleOwn(val index: Int) : EntryVersionAction

    /** The fab with the clock: asks first. */
    data object Restore : EntryVersionAction

    data object DialogConfirm : EntryVersionAction

    data object DialogDismiss : EntryVersionAction

    /** The snackbar took the message. */
    data object MessageShown : EntryVersionAction
}

/** What the snackbar says. */
enum class EntryVersionMessage { LoadFailed, NotSaved, Unexpected }
