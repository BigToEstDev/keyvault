package ru.kino.dev.settings.pres.lock

import ru.kino.dev.settings.lock.AutoLockTimeout

sealed interface LockAction {

    /** The screen is shown: what is chosen is read. */
    data object Load : LockAction

    data object Back : LockAction

    /** "Auto-lock" tapped: the wheel of its times. */
    data object ChooseTimeout : LockAction

    /** The wheel closed without a choice. */
    data object DismissTimeout : LockAction

    /** A time chosen on the wheel: kept at once. */
    data class SelectTimeout(val timeout: AutoLockTimeout) : LockAction

    /** "Lock on leaving the app" switched: kept at once. */
    data class SetLockOnLeave(val on: Boolean) : LockAction

    /** "Lock button" switched: kept at once. */
    data class SetLockButton(val on: Boolean) : LockAction

    /** The snackbar took the message. */
    data object MessageShown : LockAction
}

/** What the snackbar says. */
enum class LockMessage {
    /** The choice could not be written: it shows what is kept. */
    NotSaved,
}
