package ru.kino.dev.settings.pres.lock

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.orbitmvi.orbit.test.testWithInternalState
import ru.kino.dev.navigation.Navigator
import ru.kino.dev.settings.biometric.FakeBiometrics
import ru.kino.dev.settings.lock.AutoLockTimeout
import ru.kino.dev.settings.lock.FakeInactivityTimer
import ru.kino.dev.settings.lock.LockSettingsRepository
import ru.kino.dev.settings.pres.testing.RecordingLog
import ru.kino.dev.settings.signin.FakeSignInSettingsRepository
import ru.kino.dev.ui.biometric.BiometricPromptResult

/** Biometrics of the database on `Lock and sign-in` (pass-docs, Step 59, item 7). */
class LockProcessorBiometricTest {

    private val journal = mutableListOf<String>()
    private val biometrics = FakeBiometrics(journal)
    private val timer = FakeInactivityTimer()

    @Test
    fun `the row is on when the password of the database is kept for biometrics`() = runTest {
        biometrics.passwords[DB] = "pw"

        processor().testWithInternalState(this) {
            containerHost.action(LockAction.Load).join()

            expectInternalState { loaded(BiometricRow(on = true)) }
        }
    }

    @Test
    fun `without strong biometrics on the phone the row says so`() = runTest {
        biometrics.available = false

        processor().testWithInternalState(this) {
            containerHost.action(LockAction.Load).join()

            expectInternalState { loaded(BiometricRow(on = false, unavailable = BiometricUnavailable.NoHardware)) }
        }
    }

    @Test
    fun `on a platform without biometrics the row says so`() = runTest {
        biometrics.supported = false
        biometrics.available = false

        processor().testWithInternalState(this) {
            containerHost.action(LockAction.Load).join()

            expectInternalState { loaded(BiometricRow(on = false, unavailable = BiometricUnavailable.Unsupported)) }
        }
    }

    @Test
    fun `a database with a key file alone has no password to keep`() = runTest {
        processor(FakeDatabaseCredentials(hasPassword = false)).testWithInternalState(this) {
            containerHost.action(LockAction.Load).join()

            expectInternalState { loaded(BiometricRow(on = false, unavailable = BiometricUnavailable.NoPassword)) }
        }
    }

    @Test
    fun `turning on asks for the password first`() = runTest {
        processor().testWithInternalState(this, LockState(biometric = BiometricRow(on = false))) {
            containerHost.action(LockAction.SetBiometric(true)).join()

            expectInternalState { copy(biometricPassword = BiometricPasswordDialog()) }
        }
    }

    @Test
    fun `a wrong password stays in its dialog and says so`() = runTest {
        processor().testWithInternalState(this, typed("other")) {
            containerHost.action(LockAction.BiometricPasswordConfirm).join()

            expectInternalState { copy(biometricPassword = biometricPassword?.copy(checking = true)) }
            expectInternalState { copy(biometricPassword = biometricPassword?.copy(checking = false, wrong = true)) }
        }

        assertTrue(journal.isEmpty())
    }

    @Test
    fun `the right password goes to the prompt, and a match keeps it`() = runTest {
        processor().testWithInternalState(this, typed("pw")) {
            containerHost.action(LockAction.BiometricPasswordConfirm).join()
            expectInternalState { copy(biometricPassword = biometricPassword?.copy(checking = true)) }
            expectInternalState { copy(biometricPassword = null) }
            val prompt = awaitSideEffect() as LockSideEffect.ShowBiometricPrompt

            containerHost.action(LockAction.BiometricPromptFinished(prompt.session, BiometricPromptResult.Succeeded)).join()
            expectInternalState { copy(biometric = BiometricRow(on = true)) }
        }

        assertEquals(listOf("startSaving", "save $DB"), journal)
        assertEquals("pw", biometrics.passwords[DB])
        // The prompt is a screen of the system the app opens itself: not leaving the app
        assertEquals(1, timer.systemScreens)
    }

    @Test
    fun `a cancelled prompt keeps nothing`() = runTest {
        processor().testWithInternalState(this, typed("pw")) {
            containerHost.action(LockAction.BiometricPasswordConfirm).join()
            expectInternalState { copy(biometricPassword = biometricPassword?.copy(checking = true)) }
            expectInternalState { copy(biometricPassword = null) }
            val prompt = awaitSideEffect() as LockSideEffect.ShowBiometricPrompt

            containerHost.action(LockAction.BiometricPromptFinished(prompt.session, BiometricPromptResult.Cancelled)).join()
        }

        assertEquals(listOf("startSaving"), journal)
        assertTrue(biometrics.passwords.isEmpty())
    }

    @Test
    fun `turning off asks, then forgets`() = runTest {
        biometrics.passwords[DB] = "pw"

        processor().testWithInternalState(this, LockState(biometric = BiometricRow(on = true))) {
            containerHost.action(LockAction.SetBiometric(false)).join()
            expectInternalState { copy(forgettingBiometric = true) }

            containerHost.action(LockAction.BiometricForgetConfirm).join()
            expectInternalState { copy(forgettingBiometric = false, biometric = BiometricRow(on = false)) }
        }

        assertEquals(listOf("forget $DB"), journal)
    }

    @Test
    fun `an unavailable row is not turned on`() = runTest {
        val noPassword = LockState(biometric = BiometricRow(on = false, unavailable = BiometricUnavailable.NoPassword))

        processor().testWithInternalState(this, noPassword) {
            containerHost.action(LockAction.SetBiometric(true)).join()
        }
    }

    private fun TestScope.processor(credentials: FakeDatabaseCredentials = FakeDatabaseCredentials()) = LockProcessor(
        dbKey = DB,
        repository = NoLockSettings,
        signIn = FakeSignInSettingsRepository(),
        biometric = biometrics,
        credentials = credentials,
        timer = timer,
        navigator = Navigator(),
        log = RecordingLog(),
    )

    private fun typed(password: String) = LockState(
        biometric = BiometricRow(on = false),
        biometricPassword = BiometricPasswordDialog(password = password),
    )

    private fun LockState.loaded(row: BiometricRow) = copy(
        timeout = AutoLockTimeout.Minutes5,
        lockOnLeave = false,
        lockButton = false,
        clearPasswordOnError = false,
        autoBiometricPrompt = false,
        biometric = row,
    )

    /** The lock settings at their defaults - not what these tests are about. */
    private object NoLockSettings : LockSettingsRepository {
        override val autoLockTimeout: Flow<AutoLockTimeout> = flowOf(AutoLockTimeout.Minutes5)
        override val lockOnLeave: Flow<Boolean> = flowOf(false)
        override val lockButton: Flow<Boolean> = flowOf(false)

        override suspend fun save(timeout: AutoLockTimeout) = Unit

        override suspend fun saveLockOnLeave(on: Boolean) = Unit

        override suspend fun saveLockButton(on: Boolean) = Unit
    }
}
