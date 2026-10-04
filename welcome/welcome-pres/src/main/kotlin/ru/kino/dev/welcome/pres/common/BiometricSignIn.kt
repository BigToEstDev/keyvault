package ru.kino.dev.welcome.pres.common

import ru.kino.dev.log.ErrorLog
import ru.kino.dev.welcome.BiometricFailure
import ru.kino.dev.welcome.BiometricRepository
import ru.kino.dev.welcome.BiometricSession
import javax.inject.Inject

/** What the screen says about biometrics, under the button. */
enum class BiometricNotice {

    /** The biometrics of the phone changed; biometric unlock is off, the password works. */
    DeviceChanged,

    /** The stored password no longer opens the database - changed elsewhere; biometric unlock is off. */
    PasswordChanged,

    /** The prompt or the key failed this time; the password works. */
    Failed,
}

/**
 * Signing in with biometrics, the same on the unlock and the locked screens: the button is there only when
 * biometrics are on for the database, the prompt only on a tap, and what it gives is the password - which
 * the screen then opens with, the usual way.
 */
class BiometricSignIn @Inject constructor(
    private val biometrics: BiometricRepository,
    private val log: ErrorLog,
) {

    /** Whether to show the button for [dbUri]. */
    suspend fun isOn(dbUri: String): Boolean =
        try {
            biometrics.isAvailable() && biometrics.isEnabled(dbUri)
        } catch (e: BiometricFailure) {
            log.unexpected(TAG, e)
            false
        }

    /** A session for the prompt, or why there is none - and whether the button stays. */
    suspend fun start(dbUri: String): Start =
        try {
            Start.Prompt(biometrics.startReading(dbUri))
        } catch (e: BiometricFailure.KeyInvalidated) {
            Start.Refused(BiometricNotice.DeviceChanged, stillOn = false)
        } catch (e: BiometricFailure.NotAvailable) {
            Start.Refused(BiometricNotice.Failed, stillOn = false)
        } catch (e: BiometricFailure) {
            log.unexpected(TAG, e)
            Start.Refused(BiometricNotice.Failed, stillOn = true)
        }

    /** The stored password after the prompt let [session] through, or null when it could not be read. */
    suspend fun password(dbUri: String, session: BiometricSession): String? =
        try {
            biometrics.read(dbUri, session)
        } catch (e: BiometricFailure) {
            log.unexpected(TAG, e)
            null
        }

    /** The stored password did not open the database - forgotten, the user signs in with the password. */
    suspend fun passwordRejected(dbUri: String) {
        try {
            biometrics.forget(dbUri)
        } catch (e: BiometricFailure) {
            log.unexpected(TAG, e)
        }
    }

    sealed interface Start {
        data class Prompt(val session: BiometricSession) : Start
        data class Refused(val notice: BiometricNotice, val stillOn: Boolean) : Start
    }

    private companion object {
        const val TAG = "BiometricSignIn"
    }
}
