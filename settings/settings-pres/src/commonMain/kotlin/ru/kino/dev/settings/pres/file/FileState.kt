package ru.kino.dev.settings.pres.file

import ru.kino.dev.vault.file.DatabaseFileInfo

/**
 * @param file the file of the database; null until read - the screen is a skeleton meanwhile
 * @param renaming the dialog of the name, while open
 * @param savingCompression the compression chosen is written into the file - the choice waits, with progress
 * @param message for the snackbar, until it takes it
 */
data class FileState(
    val file: DatabaseFileInfo? = null,
    val renaming: RenameDialog? = null,
    val savingCompression: Boolean = false,
    val message: FileMessage? = null,
)

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
