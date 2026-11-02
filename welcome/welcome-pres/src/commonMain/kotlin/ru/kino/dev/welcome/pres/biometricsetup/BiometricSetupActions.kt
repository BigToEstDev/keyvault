package ru.kino.dev.welcome.pres.biometricsetup

import ru.kino.dev.welcome.pres.common.BiometricOutcome

/**
 * What the screen offering biometrics is asked to do - done by [BiometricSetupProcessor]: the layout's, and what the
 * prompt of the system hands back (pass-docs `docs/app/mvi-arc.md`).
 */
interface BiometricSetupActions {

    /** "Turn on": the prompt, then the password of the new database is encrypted and stored. */
    fun onEnable()

    fun onPromptFinished(outcome: BiometricOutcome)

    /** "Skip" - and "back" too: the database is created and open, there is nowhere to go back to. */
    fun onSkip()
}
