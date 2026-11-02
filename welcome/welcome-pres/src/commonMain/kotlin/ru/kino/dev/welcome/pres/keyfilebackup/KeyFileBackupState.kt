package ru.kino.dev.welcome.pres.keyfilebackup

import ru.kino.dev.welcome.pres.create.CreateStage

/** Where the key file of the new database came from. */
sealed interface KeyFileChoice {

    /** Generated and written by us - a `.keyx`. */
    data object Created : KeyFileChoice

    /** A file already on the device; [name] to show, when the provider tells it. */
    data class Picked(val name: String?) : KeyFileChoice
}

/** @param keyFile the key file is ready - created or picked; what is left is the database */
data class KeyFileBackupState(
    val keyFile: KeyFileChoice? = null,
    val stage: CreateStage = CreateStage(),
)
