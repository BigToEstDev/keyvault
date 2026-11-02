package ru.kino.dev.settings.pres.file

import ru.kino.dev.core.Compression

/** What the settings of the file is asked to do - done by [FileProcessor] (pass-docs, `docs/app/mvi-arc.md`). */
interface FileActions {

    /** The screen is shown: the file of the database is read. */
    fun onLoad()

    fun onBack()

    /** "Database name" tapped: the dialog with the name as it is. */
    fun onRename()

    /** Typed in the dialog of the name. */
    fun onRenameChange(name: String)

    /** "Save" in the dialog: the name is kept and the file written. */
    fun onRenameConfirm()

    fun onRenameDismiss()

    /** A compression chosen: kept and the file written at once. */
    fun onSelectCompression(compression: Compression)

    /** The snackbar took the message. */
    fun onMessageShown()
}
