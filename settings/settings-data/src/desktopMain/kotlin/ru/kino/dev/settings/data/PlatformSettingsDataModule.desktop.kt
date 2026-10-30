package ru.kino.dev.settings.data

import org.koin.core.module.Module
import org.koin.dsl.binds
import org.koin.dsl.module
import ru.kino.dev.settings.biometric.BiometricCleanup
import ru.kino.dev.settings.biometric.BiometricRepository
import ru.kino.dev.settings.data.biometric.UnavailableBiometrics
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds

/** No biometrics on the desktop yet - Windows Hello / Touch ID are not planned yet (pass-docs). */
internal actual val platformSettingsDataModule: Module = module {
    single { UnavailableBiometrics() } binds arrayOf(BiometricRepository::class, BiometricCleanup::class)
}

// Monotonic: a change of the clock of the system does not move it
internal actual fun sinceBoot(): Duration = System.nanoTime().nanoseconds
