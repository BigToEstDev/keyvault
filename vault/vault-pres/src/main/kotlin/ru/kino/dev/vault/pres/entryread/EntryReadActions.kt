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

    /** The snackbar took the message. */
    data object MessageShown : EntryReadAction
}

/** What the snackbar says. */
enum class EntryReadMessage { LoadFailed }
