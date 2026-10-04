package ru.kino.dev.welcome.pres.keyfilebackup

sealed interface KeyFileBackupAction {

    /** The main button: create a new key file, or - once there is one - choose where the database goes. */
    data object Continue : KeyFileBackupAction

    /** Use a file already on the device as the key file - one from the desktop, or shared by databases. */
    data object PickExisting : KeyFileBackupAction

    /** "Save as" handed back where to write a new key file. */
    data class KeyFilePicked(val uri: String) : KeyFileBackupAction

    /** An existing file was picked as the key file. */
    data class ExistingPicked(val uri: String) : KeyFileBackupAction

    data class DatabasePicked(val uri: String) : KeyFileBackupAction

    data object OverwriteConfirmed : KeyFileBackupAction

    data object OverwriteDismissed : KeyFileBackupAction
}

sealed interface KeyFileBackupSideEffect {

    /** "Save as" for a new key file. */
    data class PickKeyFile(val suggestedName: String) : KeyFileBackupSideEffect

    /** The picker of an existing file, for the key file. */
    data object PickExistingKeyFile : KeyFileBackupSideEffect

    /** "Save as" for the database. */
    data class PickDatabase(val suggestedName: String) : KeyFileBackupSideEffect
}
