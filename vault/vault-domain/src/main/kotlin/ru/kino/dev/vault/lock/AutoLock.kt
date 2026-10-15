package ru.kino.dev.vault.lock

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import ru.kino.dev.core.CoreException
import ru.kino.dev.vault.VaultDatabaseRepository
import kotlin.time.Duration

/** The time since some fixed point, counting while the phone sleeps - so a lock is due after a night. */
fun interface ElapsedClock {
    fun now(): Duration
}

/**
 * Locks the open database once nobody has touched the app for [timeout] (pass-docs, Step 46).
 *
 * One rule: put away, the screen off, another app on top - no touches all the same, and the same timer runs
 * out. The timer lives in the process; a process killed takes the database with it, and the next start asks
 * for the password anyway. A process frozen in the background sleeps through its timer - so coming back
 * checks the clock and locks before anything is shown.
 *
 * Locking waits for a change being written ([DatabaseGuard]). Once locked, the database is forgotten here
 * until `vault` shows it again ([watch]); [locked] tells the app to put the locked screen up, and
 * [covering] says the screens of `vault` are not to be seen meanwhile.
 *
 * @param onFailure a lock the core refused; the database stays open and is not watched any more
 */
class AutoLock(
    private val database: VaultDatabaseRepository,
    private val guard: DatabaseGuard,
    private val clock: ElapsedClock,
    private val scope: CoroutineScope,
    private val timeout: Duration,
    private val onFailure: (CoreException) -> Unit,
) {
    private val _covering = MutableStateFlow(false)

    /**
     * A lock is under way or done, and `vault` is not to be seen: from the lock until `vault` shows the
     * database again.
     */
    val covering: StateFlow<Boolean> = _covering.asStateFlow()

    private val _locked = Channel<String>(Channel.BUFFERED)

    /** The dbKey of a database just locked - once each, kept until somebody takes it. */
    val locked: Flow<String> = _locked.receiveAsFlow()

    private val state = Any()

    // Written under [state]; read without it on the touch path, where a stale value is a touch late at most
    @Volatile
    private var dbKey: String? = null

    @Volatile
    private var lastTouch: Duration = Duration.ZERO

    private var timer: Job? = null

    /** `vault` shows the database [dbKey]: from now on it is locked after [timeout] without a touch. */
    fun watch(dbKey: String) {
        synchronized(state) {
            this.dbKey = dbKey
            lastTouch = clock.now()
            _covering.value = false
            timer?.cancel()
            timer = scope.launch { countDown(dbKey) }
        }
    }

    /** A touch or a key - the person is here. */
    fun touched() {
        lastTouch = clock.now()
    }

    /** The app is on the screen again: if the timer was slept through, the lock is due now. */
    fun cameBack() {
        val key = dbKey ?: return
        if (clock.now() - lastTouch < timeout) return
        _covering.value = true
        scope.launch { lock(key) }
    }

    /** Locks now, not waiting for the timer. False when there was nothing to lock, or the core refused. */
    suspend fun lockNow(): Boolean {
        val key = dbKey ?: return false
        _covering.value = true
        // In the scope of the lock, not of the caller: the screen asking goes away with the lock it asked for
        return scope.async { lock(key) }.await()
    }

    private suspend fun countDown(key: String) {
        while (true) {
            val left = timeout - (clock.now() - lastTouch)
            if (left <= Duration.ZERO) {
                _covering.value = true
                lock(key)
                return
            }
            delay(left)
        }
    }

    private suspend fun lock(key: String): Boolean {
        val running = currentCoroutineContext()[Job]
        synchronized(state) {
            // Locked already, by another way to it, or another database is open now
            if (dbKey != key) return false
            dbKey = null
            // Not the timer the lock runs in: cancelling that one would cancel the lock
            timer?.takeIf { it != running }?.cancel()
            timer = null
        }
        return try {
            guard.exclusive { database.lock(key) }
            _locked.send(key)
            true
        } catch (e: CoreException) {
            _covering.value = false
            onFailure(e)
            false
        }
    }
}
