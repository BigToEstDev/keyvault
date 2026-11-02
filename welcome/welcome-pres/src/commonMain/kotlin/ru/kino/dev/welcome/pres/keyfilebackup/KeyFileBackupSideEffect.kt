package ru.kino.dev.welcome.pres.keyfilebackup

sealed interface KeyFileBackupSideEffect {

    /** "Save as" for a new key file. */
    data class PickKeyFile(val suggestedName: String) : KeyFileBackupSideEffect

    /** The picker of an existing file, for the key file. */
    data object PickExistingKeyFile : KeyFileBackupSideEffect

    /** "Save as" for the database. */
    data class PickDatabase(val suggestedName: String) : KeyFileBackupSideEffect
}
