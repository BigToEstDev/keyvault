package ru.kino.dev.welcome.pres.keyfilebackup

/**
 * What the screen of the key file of a new database is asked to do - done by [KeyFileBackupProcessor]: the layout's,
 * and what comes back from the pickers and the question before writing over a file (pass-docs `docs/app/mvi-arc.md`).
 */
interface KeyFileBackupActions {

    /** The main button: create a new key file, or - once there is one - "next", where the database goes. */
    fun onContinue()

    /** Use a file already on the device as the key file - one from the desktop, or shared by databases. */
    fun onPickExisting()

    /** "Save as" handed back where to write a new key file. */
    fun onKeyFilePicked(uri: String)

    /** An existing file was picked as the key file. */
    fun onExistingPicked(uri: String)

    fun onDatabasePicked(uri: String)

    fun onOverwriteConfirmed()

    fun onOverwriteDismissed()

    /** "OK" in the dialog of a creation cut off by the killed process: back to the master password. */
    fun onInterruptedDismissed()
}
