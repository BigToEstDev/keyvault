package ru.kino.dev.settings.pres.file

import ru.kino.dev.core.Compression

sealed interface FileAction {

    /** The screen is shown: the file of the database is read. */
    data object Load : FileAction

    data object Back : FileAction

    /** "Database name" tapped: the dialog with the name as it is. */
    data object Rename : FileAction

    /** Typed in the dialog of the name. */
    data class RenameChanged(val name: String) : FileAction

    /** "Save" in the dialog: the name is kept and the file written. */
    data object RenameConfirm : FileAction

    data object RenameDismiss : FileAction

    /** A compression chosen: kept and the file written at once. */
    data class SelectCompression(val compression: Compression) : FileAction

    /** The snackbar took the message. */
    data object MessageShown : FileAction
}

/** What the snackbar says. */
enum class FileMessage {
    /** The change is made, but the file was not written: it waits for the next save. */
    NotSaved,

    /** The core refused - the database closed meanwhile, or a bug. */
    Failed,
}

/**
 * The dialog of the name of the database.
 *
 * @param saving the name is kept and the file written: progress on the button, nothing takes taps
 * @param empty the name was refused as empty - said in the field, the dialog stays
 */
data class RenameDialog(val name: String, val saving: Boolean = false, val empty: Boolean = false)
