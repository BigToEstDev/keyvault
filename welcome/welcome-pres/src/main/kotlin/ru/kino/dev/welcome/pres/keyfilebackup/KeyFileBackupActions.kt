package ru.kino.dev.welcome.pres.keyfilebackup

sealed interface KeyFileBackupAction {

    /** The one button: save the key file, or - once it is saved - choose where the database goes. */
    data object Continue : KeyFileBackupAction

    data class KeyFilePicked(val uri: String) : KeyFileBackupAction

    data class DatabasePicked(val uri: String) : KeyFileBackupAction

    data object OverwriteConfirmed : KeyFileBackupAction

    data object OverwriteDismissed : KeyFileBackupAction
}

sealed interface KeyFileBackupSideEffect {

    /** "Save as" for the key file. */
    data class PickKeyFile(val suggestedName: String) : KeyFileBackupSideEffect

    /** "Save as" for the database. */
    data class PickDatabase(val suggestedName: String) : KeyFileBackupSideEffect
}
