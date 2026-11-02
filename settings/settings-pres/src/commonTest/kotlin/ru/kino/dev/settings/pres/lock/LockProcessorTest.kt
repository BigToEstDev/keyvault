package ru.kino.dev.settings.pres.lock

import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.orbitmvi.orbit.test.testWithInternalState
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.Navigator
import ru.kino.dev.settings.biometric.FakeBiometrics
import ru.kino.dev.settings.lock.AutoLockTimeout
import ru.kino.dev.settings.lock.FakeInactivityTimer
import ru.kino.dev.settings.lock.LockSettingsRepository
import ru.kino.dev.settings.pres.testing.RecordingLog
import ru.kino.dev.settings.pres.testing.runToEnd
import ru.kino.dev.settings.signin.FakeSignInSettingsRepository

class LockProcessorTest {

    @Test
    fun `the screen shown reads the chosen time`() = runTest {
        val repository = FakeLockSettingsRepository(AutoLockTimeout.Minutes10)

        lockProcessor(repository, FakeSignInSettingsRepository(), Navigator()).testWithInternalState(this) {
            containerHost.runToEnd { onLoad() }

            expectInternalState { copy(
                    timeout = AutoLockTimeout.Minutes10,
                    lockOnLeave = false,
                    lockButton = false,
                    clearPasswordOnError = false,
                    autoBiometricPrompt = false,
                    biometric = BiometricRow(on = false),
                ) }
        }
    }

    @Test
    fun `auto-lock tapped opens the wheel`() = runTest {
        val repository = FakeLockSettingsRepository(AutoLockTimeout.Minutes5)

        lockProcessor(repository, FakeSignInSettingsRepository(), Navigator()).testWithInternalState(this, LockState(AutoLockTimeout.Minutes5)) {
            containerHost.runToEnd { onChooseTimeout() }

            expectInternalState { copy(choosingTimeout = true) }
        }
    }

    @Test
    fun `the wheel does not open before the time is read`() = runTest {
        val repository = FakeLockSettingsRepository(AutoLockTimeout.Minutes5)

        lockProcessor(repository, FakeSignInSettingsRepository(), Navigator()).testWithInternalState(this) {
            containerHost.runToEnd { onChooseTimeout() }
        }
    }

    @Test
    fun `a time chosen closes the wheel and is kept at once`() = runTest {
        val repository = FakeLockSettingsRepository(AutoLockTimeout.Minutes5)
        val initial = LockState(AutoLockTimeout.Minutes5, choosingTimeout = true)

        lockProcessor(repository, FakeSignInSettingsRepository(), Navigator()).testWithInternalState(this, initial) {
            containerHost.runToEnd { onSelectTimeout(AutoLockTimeout.Seconds30) }

            expectInternalState { copy(choosingTimeout = false) }
            expectInternalState { copy(timeout = AutoLockTimeout.Seconds30) }
        }

        assertEquals(listOf(AutoLockTimeout.Seconds30), repository.saved)
    }

    @Test
    fun `a time not written shows what is kept, with a message`() = runTest {
        val repository = FakeLockSettingsRepository(AutoLockTimeout.Minutes15, failSave = true)
        val initial = LockState(AutoLockTimeout.Minutes5, choosingTimeout = true)

        lockProcessor(repository, FakeSignInSettingsRepository(), Navigator()).testWithInternalState(this, initial) {
            containerHost.runToEnd { onSelectTimeout(AutoLockTimeout.Minute1) }

            expectInternalState { copy(choosingTimeout = false) }
            expectInternalState { copy(timeout = AutoLockTimeout.Minute1) }
            expectInternalState { copy(timeout = AutoLockTimeout.Minutes15, message = LockMessage.NotSaved) }
        }
    }

    @Test
    fun `the same time chosen is not written again`() = runTest {
        val repository = FakeLockSettingsRepository(AutoLockTimeout.Minutes5)
        val initial = LockState(AutoLockTimeout.Minutes5, choosingTimeout = true)

        lockProcessor(repository, FakeSignInSettingsRepository(), Navigator()).testWithInternalState(this, initial) {
            containerHost.runToEnd { onSelectTimeout(AutoLockTimeout.Minutes5) }

            expectInternalState { copy(choosingTimeout = false) }
        }

        assertEquals(emptyList<AutoLockTimeout>(), repository.saved)
    }

    @Test
    fun `locking on leaving switched is kept at once`() = runTest {
        val repository = FakeLockSettingsRepository(AutoLockTimeout.Minutes5)

        lockProcessor(repository, FakeSignInSettingsRepository(), Navigator())
            .testWithInternalState(this, LockState(AutoLockTimeout.Minutes5, lockOnLeave = false)) {
                containerHost.runToEnd { onSetLockOnLeave(true) }

                expectInternalState { copy(lockOnLeave = true) }
            }

        assertEquals(listOf(true), repository.savedLockOnLeave)
    }

    @Test
    fun `locking on leaving not written shows what is kept, with a message`() = runTest {
        val repository = FakeLockSettingsRepository(AutoLockTimeout.Minutes5, failSave = true)

        lockProcessor(repository, FakeSignInSettingsRepository(), Navigator())
            .testWithInternalState(this, LockState(AutoLockTimeout.Minutes5, lockOnLeave = false)) {
                containerHost.runToEnd { onSetLockOnLeave(true) }

                expectInternalState { copy(lockOnLeave = true) }
                expectInternalState { copy(lockOnLeave = false, message = LockMessage.NotSaved) }
            }
    }

    @Test
    fun `the lock button switched is kept at once`() = runTest {
        val repository = FakeLockSettingsRepository(AutoLockTimeout.Minutes5)

        lockProcessor(repository, FakeSignInSettingsRepository(), Navigator())
            .testWithInternalState(this, LockState(AutoLockTimeout.Minutes5, lockButton = false)) {
                containerHost.runToEnd { onSetLockButton(true) }

                expectInternalState { copy(lockButton = true) }
            }

        assertEquals(listOf(true), repository.savedLockButton)
    }

    @Test
    fun `the lock button not written shows what is kept, with a message`() = runTest {
        val repository = FakeLockSettingsRepository(AutoLockTimeout.Minutes5, failSave = true)

        lockProcessor(repository, FakeSignInSettingsRepository(), Navigator())
            .testWithInternalState(this, LockState(AutoLockTimeout.Minutes5, lockButton = false)) {
                containerHost.runToEnd { onSetLockButton(true) }

                expectInternalState { copy(lockButton = true) }
                expectInternalState { copy(lockButton = false, message = LockMessage.NotSaved) }
            }
    }

    @Test
    fun `clearing the password switched is kept at once`() = runTest {
        val signIn = FakeSignInSettingsRepository()

        lockProcessor(FakeLockSettingsRepository(AutoLockTimeout.Minutes5), signIn, Navigator())
            .testWithInternalState(this, LockState(AutoLockTimeout.Minutes5, clearPasswordOnError = false)) {
                containerHost.runToEnd { onSetClearPasswordOnError(true) }

                expectInternalState { copy(clearPasswordOnError = true) }
            }

        assertEquals(listOf(true), signIn.savedClearPasswordOnError)
    }

    @Test
    fun `clearing the password not written shows what is kept, with a message`() = runTest {
        val signIn = FakeSignInSettingsRepository(failSave = true)

        lockProcessor(FakeLockSettingsRepository(AutoLockTimeout.Minutes5), signIn, Navigator())
            .testWithInternalState(this, LockState(AutoLockTimeout.Minutes5, clearPasswordOnError = false)) {
                containerHost.runToEnd { onSetClearPasswordOnError(true) }

                expectInternalState { copy(clearPasswordOnError = true) }
                expectInternalState { copy(clearPasswordOnError = false, message = LockMessage.NotSaved) }
            }
    }

    @Test
    fun `the auto prompt switched is kept at once`() = runTest {
        val signIn = FakeSignInSettingsRepository()

        lockProcessor(FakeLockSettingsRepository(AutoLockTimeout.Minutes5), signIn, Navigator())
            .testWithInternalState(this, LockState(AutoLockTimeout.Minutes5, autoBiometricPrompt = false)) {
                containerHost.runToEnd { onSetAutoBiometricPrompt(true) }

                expectInternalState { copy(autoBiometricPrompt = true) }
            }

        assertEquals(listOf(true), signIn.savedAutoBiometricPrompt)
    }

    @Test
    fun `the auto prompt not written shows what is kept, with a message`() = runTest {
        val signIn = FakeSignInSettingsRepository(failSave = true)

        lockProcessor(FakeLockSettingsRepository(AutoLockTimeout.Minutes5), signIn, Navigator())
            .testWithInternalState(this, LockState(AutoLockTimeout.Minutes5, autoBiometricPrompt = false)) {
                containerHost.runToEnd { onSetAutoBiometricPrompt(true) }

                expectInternalState { copy(autoBiometricPrompt = true) }
                expectInternalState { copy(autoBiometricPrompt = false, message = LockMessage.NotSaved) }
            }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `back leaves the screen`() = runTest {
        val navigator = Navigator()
        val events = mutableListOf<MviNavEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { navigator.events.collect { events += it } }

        lockProcessor(FakeLockSettingsRepository(AutoLockTimeout.Minutes5), FakeSignInSettingsRepository(), navigator).testWithInternalState(this) {
            containerHost.runToEnd { onBack() }
        }

        assertEquals(listOf<MviNavEvent>(MviNavEvent.NavigateBack), events)
    }

    private fun lockProcessor(
        repository: LockSettingsRepository,
        signIn: FakeSignInSettingsRepository,
        navigator: Navigator,
    ) = LockProcessor(
        dbKey = DB,
        repository = repository,
        signIn = signIn,
        biometric = FakeBiometrics(),
        credentials = FakeDatabaseCredentials(),
        timer = FakeInactivityTimer(),
        navigator = navigator,
        log = RecordingLog(),
    )

    private class FakeLockSettingsRepository(
        initial: AutoLockTimeout,
        private val failSave: Boolean = false,
    ) : LockSettingsRepository {

        private val timeout = MutableStateFlow(initial)

        val saved = mutableListOf<AutoLockTimeout>()

        private val leaving = MutableStateFlow(false)

        val savedLockOnLeave = mutableListOf<Boolean>()

        private val button = MutableStateFlow(false)

        val savedLockButton = mutableListOf<Boolean>()

        override val autoLockTimeout: Flow<AutoLockTimeout> = timeout

        override val lockOnLeave: Flow<Boolean> = leaving

        override val lockButton: Flow<Boolean> = button

        override suspend fun save(timeout: AutoLockTimeout) {
            if (failSave) throw IOException("disk full")
            saved += timeout
            this.timeout.value = timeout
        }

        override suspend fun saveLockOnLeave(on: Boolean) {
            if (failSave) throw IOException("disk full")
            savedLockOnLeave += on
            leaving.value = on
        }

        override suspend fun saveLockButton(on: Boolean) {
            if (failSave) throw IOException("disk full")
            savedLockButton += on
            button.value = on
        }
    }
}
