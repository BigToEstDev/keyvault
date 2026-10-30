package ru.kino.dev.settings.data.lock

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import ru.kino.dev.settings.lock.AutoLockTimeout
import ru.kino.dev.settings.lock.ElapsedClock
import ru.kino.dev.settings.lock.InactivityTimer
import kotlin.time.Duration

/**
 * The [InactivityTimer] of the app: counts on [clock], waits in [scope], takes its time from [timeouts] and
 * whether leaving locks from [lockOnLeave].
 *
 * The count lives in the process; a process killed takes the database with it, and the next start asks for the
 * password anyway. Until the setting is read it counts the default. A new time is counted from the moment it
 * comes: the old count is dropped (pass-docs, Step 59).
 */
internal class ClockInactivityTimer(
    private val clock: ElapsedClock,
    private val scope: CoroutineScope,
    timeouts: Flow<Duration>,
    lockOnLeave: Flow<Boolean>,
) : InactivityTimer {

    private val state = Any()

    // Written under [state]; read without it on the touch path, where a stale value is a touch late at most
    @Volatile
    private var timeout: Duration = AutoLockTimeout.Default.duration

    @Volatile
    private var lastTouch: Duration = Duration.ZERO

    @Volatile
    private var running = false

    @Volatile
    private var onExpired: (() -> Unit)? = null

    // Off until read: the setting is off until chosen
    @Volatile
    private var leavingLocks = false

    // The app went to a screen of the system itself: that going away is not leaving
    @Volatile
    private var toSystemScreen = false

    private var countdown: Job? = null

    init {
        scope.launch {
            timeouts.collect { chosen ->
                synchronized(state) {
                    if (chosen == timeout) return@collect
                    timeout = chosen
                    if (running) restart()
                }
            }
        }
        scope.launch { lockOnLeave.collect { leavingLocks = it } }
    }

    override fun setOnExpired(listener: (() -> Unit)?) {
        onExpired = listener
    }

    override fun start() {
        synchronized(state) {
            running = true
            restart()
        }
    }

    override fun stop() {
        synchronized(state) {
            running = false
            countdown?.cancel()
            countdown = null
        }
    }

    override fun touched() {
        lastTouch = clock.now()
    }

    override fun cameBack() {
        toSystemScreen = false
        if (running && clock.now() - lastTouch >= timeout) expire()
    }

    override fun wentAway() {
        if (toSystemScreen || !leavingLocks) return
        expire()
    }

    override fun leavingForSystemScreen() {
        toSystemScreen = true
    }

    override fun wokeUp() {
        expire()
    }

    // Under [state]
    private fun restart() {
        lastTouch = clock.now()
        countdown?.cancel()
        countdown = scope.launch { countDown() }
    }

    private suspend fun countDown() {
        while (true) {
            val left = timeout - (clock.now() - lastTouch)
            if (left <= Duration.ZERO) {
                expire()
                return
            }
            delay(left)
        }
    }

    private fun expire() {
        synchronized(state) {
            // Run out already, by the other way to it, or stopped meanwhile
            if (!running) return
            running = false
            countdown?.cancel()
            countdown = null
        }
        onExpired?.invoke()
    }
}
