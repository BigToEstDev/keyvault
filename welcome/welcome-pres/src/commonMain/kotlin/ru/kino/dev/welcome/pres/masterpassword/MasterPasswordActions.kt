package ru.kino.dev.welcome.pres.masterpassword

sealed interface MasterPasswordAction {

    data class PasswordChanged(val password: String) : MasterPasswordAction

    data class ConfirmChanged(val confirm: String) : MasterPasswordAction

    /** The confirmation field lost focus - from now on a mismatch is shown. */
    data object ConfirmLeft : MasterPasswordAction

    data object TogglePasswordVisible : MasterPasswordAction

    data class KeyFileToggled(val enabled: Boolean) : MasterPasswordAction

    data object Next : MasterPasswordAction

    data class DatabasePicked(val uri: String) : MasterPasswordAction

    data object OverwriteConfirmed : MasterPasswordAction

    data object OverwriteDismissed : MasterPasswordAction
}

sealed interface MasterPasswordSideEffect {

    /** "Save as" for the database, with the name to suggest. */
    data class PickDatabase(val suggestedName: String) : MasterPasswordSideEffect
}
