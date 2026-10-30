package ru.kino.dev.desktop

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import ru.kino.dev.settings.lock.InactivityTimer

/**
 * Sees the system slept and tells the [timer] after waking (pass-docs, Step 89): asleep, the process does not run,
 * so a tick comes far later than due. The same on Windows, Linux and macOS - no event of the system needed.
 *
 * Counts on [clock], the time of the system, which goes on in sleep on all of them; the elapsed time of the jvm
 * stops in sleep on Linux and macOS. A clock put forward by hand looks like sleep too - a lock, the safe side.
 */
internal class SleepWatch(
    private val timer: InactivityTimer,
    private val clock: () -> Duration = { System.currentTimeMillis().milliseconds },
) {

    /** Ticks until cancelled. */
    suspend fun watch() {
        var last = clock()
        while (true) {
            delay(TICK)
            val now = clock()
            if (now - last > ASLEEP) timer.wokeUp()
            last = now
        }
    }

    private companion object {
        val TICK = 2.seconds

        // A gap between two ticks longer than this - the system slept. A shorter sleep is left to the timeout
        val ASLEEP = 30.seconds
    }
}
