package ru.kino.dev.settings.data.biometric

import ru.kino.dev.settings.biometric.BiometricCleanup
import ru.kino.dev.settings.biometric.BiometricFailure
import ru.kino.dev.settings.biometric.BiometricRepository
import ru.kino.dev.settings.biometric.BiometricSession

/**
 * Biometrics of the desktop until Windows Hello / Touch ID come (pass-docs, not planned yet): never available, so the
 * screens offer nothing and the sign-in is the password. Nothing is stored, so there is nothing to forget.
 */
internal class UnavailableBiometrics : BiometricRepository, BiometricCleanup {

    override fun isAvailable(): Boolean = false

    override suspend fun isEnabled(dbUri: String): Boolean = false

    override suspend fun startSaving(): BiometricSession = throw BiometricFailure.NotAvailable()

    override suspend fun save(dbUri: String, password: String, session: BiometricSession) =
        throw BiometricFailure.NotAvailable()

    override suspend fun startReading(dbUri: String): BiometricSession = throw BiometricFailure.NotAvailable()

    override suspend fun read(dbUri: String, session: BiometricSession): String = throw BiometricFailure.NotAvailable()

    override suspend fun forget(dbUri: String) = Unit

    override suspend fun forgetOtherThan(dbUri: String) = Unit

    override suspend fun forgetAll() = Unit
}
