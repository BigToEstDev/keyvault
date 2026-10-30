package ru.kino.dev.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import ru.kino.dev.settings.lock.FakeInactivityTimer

@OptIn(ExperimentalCoroutinesApi::class)
class SleepWatchTest {

    /** The clock of the tests: the virtual time, plus what the system slept through - the ticks did not see it. */
    private var slept = Duration.ZERO

    private val timer = FakeInactivityTimer()

    @Test
    fun `ticking on time says nothing`() = runTest {
        watch()

        advanceTimeBy(10.minutes)

        assertEquals(0, timer.wakings)
    }

    @Test
    fun `a gap over 30 seconds is waking up, told once`() = runTest {
        watch()

        // The gap is the tick and the sleep: 2 + 29 seconds
        slept = 29.seconds
        advanceTimeBy(2.seconds + 1.milliseconds)
        assertEquals(1, timer.wakings)

        advanceTimeBy(10.minutes)
        assertEquals(1, timer.wakings)
    }

    @Test
    fun `a gap of 30 seconds is not sleep`() = runTest {
        watch()

        slept = 28.seconds
        advanceTimeBy(2.seconds + 1.milliseconds)

        assertEquals(0, timer.wakings)
    }

    private fun TestScope.watch() {
        val watch = SleepWatch(timer, clock = { testScheduler.currentTime.milliseconds + slept })
        backgroundScope.launch { watch.watch() }
        runCurrent()
    }
}
