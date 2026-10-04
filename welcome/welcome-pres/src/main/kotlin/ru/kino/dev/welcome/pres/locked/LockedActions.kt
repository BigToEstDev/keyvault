package ru.kino.dev.welcome.pres.locked

sealed interface LockedAction {

    data class PasswordChanged(val password: String) : LockedAction

    data object TogglePasswordVisible : LockedAction

    data object Submit : LockedAction

    data object AttachKeyFile : LockedAction

    data class KeyFilePicked(val uri: String) : LockedAction

    data object RemoveKeyFile : LockedAction
}

sealed interface LockedSideEffect {

    data object PickKeyFile : LockedSideEffect
}
