package ru.kino.dev.welcome.pres.masterpassword

import ru.kino.dev.core.CredentialLimits
import ru.kino.dev.welcome.pres.create.CreateStage

/**
 * @param confirmLeft the confirmation field lost focus once - a mismatch is shown only after that, not
 * while it is being typed
 * @param tooLong the last thing typed was over the limit and was not taken
 */
data class MasterPasswordState(
    val password: String = "",
    val confirm: String = "",
    val passwordVisible: Boolean = false,
    val confirmLeft: Boolean = false,
    val tooLong: Boolean = false,
    val withKeyFile: Boolean = false,
    val stage: CreateStage = CreateStage(),
) {

    /** Shorter than the minimum - said under the field while it is, and "next" stays off. */
    val tooShort: Boolean = CredentialLimits.isPasswordTooShort(password)

    /**
     * The confirmation is required whether the password is shown or not: looking at a password and
     * confirming it are different things.
     */
    val canGoOn: Boolean = !tooShort && password == confirm && !stage.busy

    val mismatch: Boolean = confirmLeft && confirm.isNotEmpty() && confirm != password
}
