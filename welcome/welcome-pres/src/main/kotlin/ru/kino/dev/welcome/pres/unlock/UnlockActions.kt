package ru.kino.dev.welcome.pres.unlock

sealed interface UnlockAction {

    data class PasswordChanged(val password: String) : UnlockAction

    data object TogglePasswordVisible : UnlockAction

    /** Open the database - also "retry" after a failure that may pass. */
    data object Submit : UnlockAction

    /** The name of the database tapped, or "pick another file" after a failure. */
    data object ChangeDatabase : UnlockAction

    data class DatabasePicked(val uri: String) : UnlockAction

    data object AttachKeyFile : UnlockAction

    data class KeyFilePicked(val uri: String) : UnlockAction

    data object RemoveKeyFile : UnlockAction
}

sealed interface UnlockSideEffect {

    data object PickDatabase : UnlockSideEffect

    data object PickKeyFile : UnlockSideEffect
}
