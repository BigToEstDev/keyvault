package ru.kino.dev.welcome.data

import ru.kino.dev.welcome.data.biometric.BiometricCleanup

/** Calls go to the shared [journal] as `"forgetBiometricsOtherThan <uri>"` and `"forgetAllBiometrics"`. */
internal class FakeBiometricCleanup(private val journal: MutableList<String>) : BiometricCleanup {

    override suspend fun forgetOtherThan(dbUri: String) {
        journal += "forgetBiometricsOtherThan $dbUri"
    }

    override suspend fun forgetAll() {
        journal += "forgetAllBiometrics"
    }
}
