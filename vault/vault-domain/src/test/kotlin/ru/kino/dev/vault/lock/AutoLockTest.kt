package ru.kino.dev.vault.lock

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.kino.dev.core.CoreException
import ru.kino.dev.vault.FakeVaultDatabase
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class AutoLockTest {

    private val database = FakeVaultDatabase()
    private val guard = DatabaseGuard()
    private val failures = mutableListOf<CoreException>()

    /** The clock of the tests, moved by hand: the virtual time, plus what the phone slept through. */
    private var slept = Duration.ZERO

    @Test
    fun `no touch for the timeout locks the database and says so`() = runTest {
        val lock = autoLock()
        lock.watch(DB)

        advanceTimeBy(TIMEOUT - 1.seconds)
        assertTrue(database.calls.isEmpty())

        advanceTimeBy(2.seconds)
        assertEquals(listOf("lock"), database.calls)
        assertTrue(lock.covering.value)
        assertEquals(DB, lock.locked.first())
    }

    @Test
    fun `a touch starts the timeout over`() = runTest {
        val lock = autoLock()
        lock.watch(DB)

        advanceTimeBy(TIMEOUT - 1.seconds)
        lock.touched()
        advanceTimeBy(TIMEOUT - 1.seconds)
        assertTrue(database.calls.isEmpty())

        advanceTimeBy(2.seconds)
        assertEquals(listOf("lock"), database.calls)
    }

    @Test
    fun `coming back after the timeout was slept through locks at once, covered`() = runTest {
        val lock = autoLock()
        lock.watch(DB)

        // The process was frozen: the clock went on, the timer did not
        slept = TIMEOUT
        lock.cameBack()
        assertTrue(lock.covering.value)
        runCurrent()

        assertEquals(listOf("lock"), database.calls)
        assertEquals(DB, lock.locked.first())
    }

    @Test
    fun `coming back before the timeout does nothing`() = runTest {
        val lock = autoLock()
        lock.watch(DB)

        slept = TIMEOUT - 1.seconds
        lock.cameBack()
        runCurrent()

        assertFalse(lock.covering.value)
        assertTrue(database.calls.isEmpty())
    }

    @Test
    fun `a lock waits for a change being written`() = runTest {
        val lock = autoLock()
        lock.watch(DB)
        val written = CompletableDeferred<Unit>()
        launch { guard.exclusive { written.await() } }
        runCurrent()

        val locking = async { lock.lockNow() }
        runCurrent()
        assertTrue(database.calls.isEmpty())

        written.complete(Unit)
        assertTrue(locking.await())
        assertEquals(listOf("lock"), database.calls)
    }

    @Test
    fun `with no database watched there is nothing to lock`() = runTest {
        val lock = autoLock()

        advanceTimeBy(TIMEOUT * 2)
        slept = TIMEOUT
        lock.cameBack()
        runCurrent()

        assertFalse(lock.lockNow())
        assertTrue(database.calls.isEmpty())
    }

    @Test
    fun `a locked database is not watched until vault shows it again`() = runTest {
        val lock = autoLock()
        lock.watch(DB)
        assertTrue(lock.lockNow())

        advanceTimeBy(TIMEOUT * 2)
        assertEquals(listOf("lock"), database.calls)

        lock.watch(DB)
        assertFalse(lock.covering.value)
        advanceTimeBy(TIMEOUT + 1.seconds)
        assertEquals(listOf("lock", "lock"), database.calls)
    }

    @Test
    fun `a lock the core refuses uncovers, is reported and stops watching`() = runTest {
        database.lockFailure = CoreException("DbKeyNotFound", "not open")
        val lock = autoLock()
        lock.watch(DB)

        assertFalse(lock.lockNow())

        assertFalse(lock.covering.value)
        assertEquals(1, failures.size)
        advanceTimeBy(TIMEOUT * 2)
        assertEquals(listOf("lock"), database.calls)
    }

    private fun TestScope.autoLock() = AutoLock(
        database = database,
        guard = guard,
        clock = { testScheduler.currentTime.milliseconds + slept },
        scope = backgroundScope,
        timeout = TIMEOUT,
        onFailure = { failures += it },
    )

    private companion object {
        const val DB = "content://docs/passwords.kdbx"
        val TIMEOUT = 5.minutes
    }
}
