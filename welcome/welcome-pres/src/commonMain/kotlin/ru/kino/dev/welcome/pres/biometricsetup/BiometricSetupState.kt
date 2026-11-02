package ru.kino.dev.welcome.pres.biometricsetup

/**
 * @param busy turning biometrics on - the prompt is out, or the password is being stored
 * @param failed turning on did not work; skipping still does
 */
data class BiometricSetupState(
    val busy: Boolean = false,
    val failed: Boolean = false,
)
