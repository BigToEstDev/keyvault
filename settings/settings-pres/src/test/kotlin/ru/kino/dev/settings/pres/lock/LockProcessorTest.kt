package ru.kino.dev.settings.pres.lock

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.orbitmvi.orbit.test.testWithInternalState
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.Navigator
import ru.kino.dev.settings.lock.AutoLockTimeout
import ru.kino.dev.settings.lock.LockSettingsRepository
import ru.kino.dev.settings.signin.FakeSignInSettingsRepository
import java.io.IOException

class LockProcessorTest {

    @Test
    fun `the screen shown reads the chosen time`() = runTest {
        val repository = FakeLockSettingsRepository(AutoLockTimeout.Minutes10)

        LockProcessor(repository, FakeSignInSettingsRepository(), Navigator()).testWithInternalState(this) {
            containerHost.action(LockAction.Load).join()

            expectInternalState { copy(
                    timeout = AutoLockTimeout.Minutes10,
                    lockOnLeave = false,
                    lockButton = false,
                    clearPasswordOnError = false,
                ) }
        }
    }

    @Test
    fun `auto-lock tapped opens the wheel`() = runTest {
        val repository = FakeLockSettingsRepository(AutoLockTimeout.Minutes5)

        LockProcessor(repository, FakeSignInSettingsRepository(), Navigator()).testWithInternalState(this, LockState(AutoLockTimeout.Minutes5)) {
            containerHost.action(LockAction.ChooseTimeout).join()

            expectInternalState { copy(choosingTimeout = true) }
        }
    }

    @Test
    fun `the wheel does not open before the time is read`() = runTest {
        val repository = FakeLockSettingsRepository(AutoLockTimeout.Minutes5)

        LockProcessor(repository, FakeSignInSettingsRepository(), Navigator()).testWithInternalState(this) {
            containerHost.action(LockAction.ChooseTimeout).join()
        }
    }

    @Test
    fun `a time chosen closes the wheel and is kept at once`() = runTest {
        val repository = FakeLockSettingsRepository(AutoLockTimeout.Minutes5)
        val initial = LockState(AutoLockTimeout.Minutes5, choosingTimeout = true)

        LockProcessor(repository, FakeSignInSettingsRepository(), Navigator()).testWithInternalState(this, initial) {
            containerHost.action(LockAction.SelectTimeout(AutoLockTimeout.Seconds30)).join()

            expectInternalState { copy(choosingTimeout = false) }
            expectInternalState { copy(timeout = AutoLockTimeout.Seconds30) }
        }

        assertEquals(listOf(AutoLockTimeout.Seconds30), repository.saved)
    }

    @Test
    fun `a time not written shows what is kept, with a message`() = runTest {
        val repository = FakeLockSettingsRepository(AutoLockTimeout.Minutes15, failSave = true)
        val initial = LockState(AutoLockTimeout.Minutes5, choosingTimeout = true)

        LockProcessor(repository, FakeSignInSettingsRepository(), Navigator()).testWithInternalState(this, initial) {
            containerHost.action(LockAction.SelectTimeout(AutoLockTimeout.Minute1)).join()

            expectInternalState { copy(choosingTimeout = false) }
            expectInternalState { copy(timeout = AutoLockTimeout.Minute1) }
            expectInternalState { copy(timeout = AutoLockTimeout.Minutes15, message = LockMessage.NotSaved) }
        }
    }

    @Test
    fun `the same time chosen is not written again`() = runTest {
        val repository = FakeLockSettingsRepository(AutoLockTimeout.Minutes5)
        val initial = LockState(AutoLockTimeout.Minutes5, choosingTimeout = true)

        LockProcessor(repository, FakeSignInSettingsRepository(), Navigator()).testWithInternalState(this, initial) {
            containerHost.action(LockAction.SelectTimeout(AutoLockTimeout.Minutes5)).join()

            expectInternalState { copy(choosingTimeout = false) }
        }

        assertEquals(emptyList<AutoLockTimeout>(), repository.saved)
    }

    @Test
    fun `locking on leaving switched is kept at once`() = runTest {
        val repository = FakeLockSettingsRepository(AutoLockTimeout.Minutes5)

        LockProcessor(repository, FakeSignInSettingsRepository(), Navigator())
            .testWithInternalState(this, LockState(AutoLockTimeout.Minutes5, lockOnLeave = false)) {
                containerHost.action(LockAction.SetLockOnLeave(true)).join()

                expectInternalState { copy(lockOnLeave = true) }
            }

        assertEquals(listOf(true), repository.savedLockOnLeave)
    }

    @Test
    fun `locking on leaving not written shows what is kept, with a message`() = runTest {
        val repository = FakeLockSettingsRepository(AutoLockTimeout.Minutes5, failSave = true)

        LockProcessor(repository, FakeSignInSettingsRepository(), Navigator())
            .testWithInternalState(this, LockState(AutoLockTimeout.Minutes5, lockOnLeave = false)) {
                containerHost.action(LockAction.SetLockOnLeave(true)).join()

                expectInternalState { copy(lockOnLeave = true) }
                expectInternalState { copy(lockOnLeave = false, message = LockMessage.NotSaved) }
            }
    }

    @Test
    fun `the lock button switched is kept at once`() = runTest {
        val repository = FakeLockSettingsRepository(AutoLockTimeout.Minutes5)

        LockProcessor(repository, FakeSignInSettingsRepository(), Navigator())
            .testWithInternalState(this, LockState(AutoLockTimeout.Minutes5, lockButton = false)) {
                containerHost.action(LockAction.SetLockButton(true)).join()

                expectInternalState { copy(lockButton = true) }
            }

        assertEquals(listOf(true), repository.savedLockButton)
    }

    @Test
    fun `the lock button not written shows what is kept, with a message`() = runTest {
        val repository = FakeLockSettingsRepository(AutoLockTimeout.Minutes5, failSave = true)

        LockProcessor(repository, FakeSignInSettingsRepository(), Navigator())
            .testWithInternalState(this, LockState(AutoLockTimeout.Minutes5, lockButton = false)) {
                containerHost.action(LockAction.SetLockButton(true)).join()

                expectInternalState { copy(lockButton = true) }
                expectInternalState { copy(lockButton = false, message = LockMessage.NotSaved) }
            }
    }

    @Test
    fun `clearing the password switched is kept at once`() = runTest {
        val signIn = FakeSignInSettingsRepository()

        LockProcessor(FakeLockSettingsRepository(AutoLockTimeout.Minutes5), signIn, Navigator())
            .testWithInternalState(this, LockState(AutoLockTimeout.Minutes5, clearPasswordOnError = false)) {
                containerHost.action(LockAction.SetClearPasswordOnError(true)).join()

                expectInternalState { copy(clearPasswordOnError = true) }
            }

        assertEquals(listOf(true), signIn.savedClearPasswordOnError)
    }

    @Test
    fun `clearing the password not written shows what is kept, with a message`() = runTest {
        val signIn = FakeSignInSettingsRepository(failSave = true)

        LockProcessor(FakeLockSettingsRepository(AutoLockTimeout.Minutes5), signIn, Navigator())
            .testWithInternalState(this, LockState(AutoLockTimeout.Minutes5, clearPasswordOnError = false)) {
                containerHost.action(LockAction.SetClearPasswordOnError(true)).join()

                expectInternalState { copy(clearPasswordOnError = true) }
                expectInternalState { copy(clearPasswordOnError = false, message = LockMessage.NotSaved) }
            }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `back leaves the screen`() = runTest {
        val navigator = Navigator()
        val events = mutableListOf<MviNavEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { navigator.events.collect { events += it } }

        LockProcessor(FakeLockSettingsRepository(AutoLockTimeout.Minutes5), FakeSignInSettingsRepository(), navigator).testWithInternalState(this) {
            containerHost.action(LockAction.Back).join()
        }

        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }

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
