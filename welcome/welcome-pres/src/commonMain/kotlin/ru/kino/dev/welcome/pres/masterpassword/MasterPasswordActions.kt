package ru.kino.dev.welcome.pres.masterpassword

/**
 * What the screen of setting the master password is asked to do - done by [MasterPasswordProcessor]: the layout's,
 * and what comes back from the picker and the question before writing over a file (pass-docs `docs/app/mvi-arc.md`).
 */
interface MasterPasswordActions {

    fun onPasswordChange(password: String)

    fun onConfirmChange(confirm: String)

    /** The confirmation field lost focus - from now on a mismatch is shown. */
    fun onConfirmLeft()

    /** One toggle for both fields. */
    fun onTogglePasswordVisible()

    fun onKeyFileToggle(enabled: Boolean)

    fun onNext()

    fun onDatabasePicked(uri: String)

    fun onOverwriteConfirmed()

    fun onOverwriteDismissed()

    /** "OK" in the dialog of a creation cut off by the killed process. */
    fun onInterruptedDismissed()
}
