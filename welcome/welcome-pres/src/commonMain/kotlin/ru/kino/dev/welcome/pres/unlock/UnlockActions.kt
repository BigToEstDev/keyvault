package ru.kino.dev.welcome.pres.unlock

import ru.kino.dev.welcome.pres.common.BiometricOutcome
import ru.kino.dev.welcome.pres.common.passwordentry.PasswordEntryActions

/**
 * What the unlock screen is asked to do - done by [UnlockProcessor]: the layout's, the choices of what is opened,
 * and what comes back from the pickers and the prompt of the system.
 */
interface UnlockActions : PasswordEntryActions {

    fun onDatabasePicked(uri: String)

    fun onKeyFilePicked(uri: String)

    fun onBiometricFinished(outcome: BiometricOutcome)
}
