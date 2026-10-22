package ru.kino.dev.settings.pres.recyclebin

sealed interface RecycleBinAction {

    /** The screen is shown: the switch and what the bin holds are read. */
    data object Load : RecycleBinAction

    data object Back : RecycleBinAction

    /** "Use the recycle bin" flipped: on - kept at once; off with something in the bin - asked first. */
    data class SetEnabled(val enabled: Boolean) : RecycleBinAction

    /** "Switch off" in the dialog: the bin goes with all it holds, the file is written. */
    data object OffConfirm : RecycleBinAction

    data object OffDismiss : RecycleBinAction

    /** "Deleted" tapped: the screen of the bin, in `vault`. */
    data object OpenTrash : RecycleBinAction

    /** The snackbar took the message. */
    data object MessageShown : RecycleBinAction
}

/** What the snackbar says. */
enum class RecycleBinMessage {
    /** The change is made, but the file was not written: it waits for the next save. */
    NotSaved,

    /** The core refused - the database closed meanwhile, or a bug. */
    Failed,
}
