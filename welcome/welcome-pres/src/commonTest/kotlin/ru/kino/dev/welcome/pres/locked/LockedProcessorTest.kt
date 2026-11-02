package ru.kino.dev.welcome.pres.locked

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.orbitmvi.orbit.test.testWithInternalState
import ru.kino.dev.navigation.LockedRoute
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.VaultRootRoute
import ru.kino.dev.settings.biometric.FakeBiometrics
import ru.kino.dev.settings.signin.FakeSignInSettingsRepository
import ru.kino.dev.welcome.CurrentDatabase
import ru.kino.dev.welcome.PickedFile
import ru.kino.dev.welcome.WelcomeFailure
import ru.kino.dev.welcome.pres.FakeCurrentDatabase
import ru.kino.dev.welcome.pres.FakeErrorLog
import ru.kino.dev.welcome.pres.FakeWelcomeDatabase
import ru.kino.dev.welcome.pres.FakeWelcomeFiles
import ru.kino.dev.welcome.pres.common.BiometricNotice
import ru.kino.dev.welcome.pres.common.BiometricOutcome
import ru.kino.dev.welcome.pres.common.BiometricSignIn
import ru.kino.dev.welcome.pres.common.EntryProblem
import ru.kino.dev.welcome.pres.common.PasswordEntry
import ru.kino.dev.welcome.pres.recordNavigation

class LockedProcessorTest {

    private val journal = mutableListOf<String>()
    private val database = FakeWelcomeDatabase(journal)
    private val files = FakeWelcomeFiles().apply { names[NEW_KEY] = "new.keyx" }
    private val biometrics = FakeBiometrics(journal)
    private val current = FakeCurrentDatabase(CurrentDatabase(DB, "passwords.kdbx", "Passwords", KEY, "my.keyx"))

    private fun TestScope.processor(signIn: FakeSignInSettingsRepository = FakeSignInSettingsRepository()) =
        recordNavigation().let { (navigator, events) ->
            val log = FakeErrorLog()
            LockedProcessor(DB, database, files, current, navigator, log, BiometricSignIn(biometrics, log), signIn) to
                events
        }

    @Test
    fun `shows the name of the database and the key file it was opened with - nothing else`() = runTest {
        val (processor, _) = processor()

        processor.testWithInternalState(this) {
            runOnCreate().join()

            expectInternalState(lockedState())
        }
    }

    @Test
    fun `unlocking starts vault again at its root`() = runTest {
        val (processor, events) = processor()

        processor.testWithInternalState(this, lockedState()) {
            containerHost.onPasswordChange("pw")
            expectInternalState { copy(entry = entry.copy(password = "pw")) }
            containerHost.onSubmit()
            expectInternalState { copy(entry = entry.copy(busy = true)) }
        }

        assertEquals(listOf("unlock $DB"), journal)
        assertEquals("pw", database.passwordReceived)
        assertEquals(KEY, database.keyFileReceived)
        assertEquals(listOf(TO_VAULT_ROOT), events)
    }

    @Test
    fun `unlocked, the screen stays waiting as it was until it is gone - not wiped for the slide`() = runTest {
        val (processor, events) = processor()
        val typed = lockedState().let { it.copy(entry = it.entry.copy(password = "pw", biometricOn = true)) }

        processor.testWithInternalState(this, typed) {
            containerHost.onSubmit()
            expectInternalState { copy(entry = entry.copy(busy = true)) }
            cancelAndIgnoreRemainingItems()
        }

        assertEquals(typed.copy(entry = typed.entry.copy(busy = true)), processor.container.stateFlow.value)
        assertEquals(listOf(TO_VAULT_ROOT), events)
    }

    @Test
    fun `an unreachable key file is answered under the key file, and another one is unlocked with`() = runTest {
        database.failure = WelcomeFailure.FileUnreachable(PickedFile.KeyFile)
        val (processor, events) = processor()
        val typed = lockedState().let { it.copy(entry = it.entry.copy(password = "pw")) }

        processor.testWithInternalState(this, typed) {
            containerHost.onSubmit()
            expectInternalState { copy(entry = entry.copy(busy = true)) }
            expectInternalState {
                copy(entry = entry.copy(busy = false, problem = EntryProblem.Unreachable(PickedFile.KeyFile)))
            }

            containerHost.onAttachKeyFile()
            expectSideEffect(LockedSideEffect.PickKeyFile)
            containerHost.onKeyFilePicked(NEW_KEY)
            expectInternalState {
                copy(entry = entry.copy(keyFileUri = NEW_KEY, keyFileName = "new.keyx", problem = null))
            }

            containerHost.onSubmit()
            expectInternalState { copy(entry = entry.copy(busy = true)) }
        }

        assertEquals(NEW_KEY, database.keyFileReceived)
        assertEquals(listOf(TO_VAULT_ROOT), events)
    }

    @Test
    fun `wrong credentials wipe the password when the settings say so`() = runTest {
        database.failure = WelcomeFailure.InvalidCredentials()
        val (processor, _) = processor(FakeSignInSettingsRepository(clearPasswordOnError = true))
        val typed = lockedState().let { it.copy(entry = it.entry.copy(password = "pw")) }

        processor.testWithInternalState(this, typed) {
            containerHost.onSubmit()
            expectInternalState { copy(entry = entry.copy(busy = true)) }
            expectInternalState {
                copy(entry = entry.copy(busy = false, password = "", problem = EntryProblem.InvalidCredentials))
            }
        }
    }

    @Test
    fun `wrong credentials keep the password when the settings do not wipe it`() = runTest {
        database.failure = WelcomeFailure.InvalidCredentials()
        val (processor, _) = processor()
        val typed = lockedState().let { it.copy(entry = it.entry.copy(password = "pw")) }

        processor.testWithInternalState(this, typed) {
            containerHost.onSubmit()
            expectInternalState { copy(entry = entry.copy(busy = true)) }
            expectInternalState { copy(entry = entry.copy(busy = false, problem = EntryProblem.InvalidCredentials)) }
        }
    }

    @Test
    fun `wrong credentials stay on the lock`() = runTest {
        database.failure = WelcomeFailure.InvalidCredentials()
        val (processor, events) = processor()

        processor.testWithInternalState(this, lockedState()) {
            containerHost.onSubmit()
            expectInternalState { copy(entry = entry.copy(busy = true)) }
            expectInternalState { copy(entry = entry.copy(busy = false, problem = EntryProblem.InvalidCredentials)) }
        }

        assertTrue(events.isEmpty())
    }

    @Test
    fun `with the auto prompt on, biometrics are asked as the lock is shown`() = runTest {
        biometrics.passwords[DB] = "stored"
        val (processor, _) = processor(FakeSignInSettingsRepository(autoBiometricPrompt = true))

        processor.testWithInternalState(this) {
            runOnCreate().join()
            expectInternalState(lockedState().let { it.copy(entry = it.entry.copy(biometricOn = true)) })
            awaitSideEffect() as LockedSideEffect.ShowBiometricPrompt
        }
    }

    @Test
    fun `with the auto prompt off, biometrics wait for the button`() = runTest {
        biometrics.passwords[DB] = "stored"
        val (processor, _) = processor()

        processor.testWithInternalState(this) {
            runOnCreate().join()
            expectInternalState(lockedState().let { it.copy(entry = it.entry.copy(biometricOn = true)) })
        }
    }

    @Test
    fun `the auto prompt asks nothing when biometrics are off for the database`() = runTest {
        val (processor, _) = processor(FakeSignInSettingsRepository(autoBiometricPrompt = true))

        processor.testWithInternalState(this) {
            runOnCreate().join()
            expectInternalState(lockedState())
        }
    }

    @Test
    fun `biometrics unlock with the stored password through the usual unlock`() = runTest {
        biometrics.passwords[DB] = "stored"
        val (processor, events) = processor()
        val on = lockedState().let { it.copy(entry = it.entry.copy(biometricOn = true)) }

        processor.testWithInternalState(this, on) {
            containerHost.onBiometric()
            val prompt = awaitSideEffect() as LockedSideEffect.ShowBiometricPrompt
            containerHost.onBiometricFinished(BiometricOutcome.Succeeded(prompt.session))
            expectInternalState { copy(entry = entry.copy(busy = true)) }
        }

        assertEquals(listOf("unlock $DB"), journal)
        assertEquals("stored", database.passwordReceived)
        assertEquals(KEY, database.keyFileReceived)
        assertEquals(listOf(TO_VAULT_ROOT), events)
    }

    @Test
    fun `a stale stored password is forgotten on the lock too`() = runTest {
        biometrics.passwords[DB] = "old"
        database.failure = WelcomeFailure.InvalidCredentials()
        val (processor, events) = processor()
        val on = lockedState().let { it.copy(entry = it.entry.copy(biometricOn = true)) }

        processor.testWithInternalState(this, on) {
            containerHost.onBiometric()
            val prompt = awaitSideEffect() as LockedSideEffect.ShowBiometricPrompt
            containerHost.onBiometricFinished(BiometricOutcome.Succeeded(prompt.session))
            expectInternalState { copy(entry = entry.copy(busy = true)) }
            expectInternalState {
                copy(entry = entry.copy(busy = false, biometricOn = false, biometricNotice = BiometricNotice.PasswordChanged))
            }
        }

        assertTrue(journal.contains("forget $DB"))
        assertEquals(BiometricNotice.PasswordChanged, processor.container.stateFlow.value.entry.biometricNotice)
        assertTrue(events.isEmpty())
    }

    private fun lockedState() = LockedState(
        databaseName = "Passwords",
        entry = PasswordEntry(keyFileUri = KEY, keyFileName = "my.keyx"),
        read = true,
    )

    private companion object {
        const val DB = "content://docs/passwords.kdbx"
        const val KEY = "content://keys/my.keyx"
        const val NEW_KEY = "content://keys/new.keyx"

        /** The lock popped `vault`: unlocking puts its root in place of the locked screen. */
        val TO_VAULT_ROOT = MviNavEvent.NavigateTo(VaultRootRoute(DB), popUpTo = LockedRoute(DB), inclusive = true)
    }
}
