package ru.kino.dev.settings.pres.lock

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** "Ask for biometrics by themselves" follows biometrics of the database (pass-docs, Step 93). */
class LockStateTest {

    @Test
    fun `with biometrics of the database on, asking by themselves can be chosen`() {
        assertTrue(LockState(biometric = BiometricRow(on = true)).autoBiometricPromptAllowed)
    }

    @Test
    fun `with them off, it cannot`() {
        assertFalse(LockState(biometric = BiometricRow(on = false)).autoBiometricPromptAllowed)
    }

    @Test
    fun `with them unavailable, it cannot`() {
        val state = LockState(biometric = BiometricRow(on = false, unavailable = BiometricUnavailable.Unsupported))

        assertFalse(state.autoBiometricPromptAllowed)
    }

    @Test
    fun `until biometrics are read, it cannot`() {
        assertFalse(LockState().autoBiometricPromptAllowed)
    }
}
