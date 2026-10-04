package ru.kino.dev.welcome.data

import ru.kino.dev.welcome.data.biometric.BiometricCleanup

/** Calls go to the shared [journal] as `"forgetBiometricsOtherThan <uri>"`. */
internal class FakeBiometricCleanup(private val journal: MutableList<String>) : BiometricCleanup {

    override suspend fun forgetOtherThan(dbUri: String) {
        journal += "forgetBiometricsOtherThan $dbUri"
    }
}
