package ru.kino.dev.welcome.pres.locked

import ru.kino.dev.welcome.pres.common.BiometricOutcome
import ru.kino.dev.welcome.pres.common.passwordentry.PasswordEntryActions

/**
 * What the locked screen is asked to do - done by [LockedProcessor]: the layout's, and what comes back from the
 * pickers and the prompt of the system. Changing the database or its key file is not done here - the database is
 * open, so those choices keep their nothing-by-default.
 */
interface LockedActions : PasswordEntryActions {

    fun onKeyFilePicked(uri: String)

    fun onBiometricFinished(outcome: BiometricOutcome)
}
