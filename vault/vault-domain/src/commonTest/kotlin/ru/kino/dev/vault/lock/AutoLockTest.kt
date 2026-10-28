package ru.kino.dev.vault.lock

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import ru.kino.dev.core.CoreException
import ru.kino.dev.settings.lock.FakeInactivityTimer
import ru.kino.dev.vault.FakeVaultDatabase

/** The lock; the count is the timer's - its own tests are in `settings-data`. */
@OptIn(ExperimentalCoroutinesApi::class)
class AutoLockTest {

    private val database = FakeVaultDatabase()
    private val guard = DatabaseGuard()
    private val timer = FakeInactivityTimer()
    private val failures = mutableListOf<CoreException>()

    @Test
    fun `a database shown starts the timer`() = runTest {
        autoLock().watch(DB)

        assertTrue(timer.running)
    }

    @Test
    fun `the timer running out covers at once, locks and says so`() = runTest {
        val lock = autoLock()
        lock.watch(DB)

        timer.expire()
        // Covered before the lock is under way: coming back, the screens are not drawn first
        assertTrue(lock.covering.value)
        runCurrent()

        assertEquals(listOf("lock"), database.calls)
        assertEquals(DB, lock.locked.first())
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

        assertFalse(lock.lockNow())
        assertTrue(database.calls.isEmpty())
    }

    @Test
    fun `a locked database stops the timer and is not watched until vault shows it again`() = runTest {
        val lock = autoLock()
        lock.watch(DB)
        assertTrue(lock.lockNow())
        assertFalse(timer.running)

        lock.watch(DB)
        assertFalse(lock.covering.value)
        timer.expire()
        runCurrent()
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
        assertFalse(timer.running)
    }

    private fun TestScope.autoLock() = AutoLock(
        database = database,
        guard = guard,
        timer = timer,
        scope = backgroundScope,
        onFailure = { failures += it },
    )

    private companion object {
        const val DB = "content://docs/passwords.kdbx"
    }
}
