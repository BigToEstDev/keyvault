package ru.kino.dev.settings.data.lock

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class ClockInactivityTimerTest {

    /** The clock of the tests, moved by hand: the virtual time, plus what the phone slept through. */
    private var slept = Duration.ZERO

    private val timeouts = MutableStateFlow(5.minutes)

    private val lockOnLeave = MutableStateFlow(false)

    private var expired = 0

    @Test
    fun `no touch for the time runs out once`() = runTest {
        val timer = timer()
        timer.start()

        advanceTimeBy(5.minutes - 1.seconds)
        assertEquals(0, expired)

        advanceTimeBy(2.seconds)
        assertEquals(1, expired)

        advanceTimeBy(20.minutes)
        assertEquals(1, expired)
    }

    @Test
    fun `a touch starts the count over`() = runTest {
        val timer = timer()
        timer.start()

        advanceTimeBy(5.minutes - 1.seconds)
        timer.touched()
        advanceTimeBy(5.minutes - 1.seconds)
        assertEquals(0, expired)

        advanceTimeBy(2.seconds)
        assertEquals(1, expired)
    }

    @Test
    fun `coming back after the count was slept through runs out before the call returns`() = runTest {
        val timer = timer()
        timer.start()

        // The process was frozen: the clock went on, the count did not
        slept = 5.minutes
        timer.cameBack()

        assertEquals(1, expired)
    }

    @Test
    fun `coming back before the time does nothing`() = runTest {
        val timer = timer()
        timer.start()

        slept = 5.minutes - 1.seconds
        timer.cameBack()

        assertEquals(0, expired)
    }

    @Test
    fun `stopped, it does not run out`() = runTest {
        val timer = timer()
        timer.start()
        timer.stop()

        advanceTimeBy(10.minutes)
        slept = 10.minutes
        timer.cameBack()

        assertEquals(0, expired)
    }

    @Test
    fun `a new time counts from the moment it is chosen`() = runTest {
        val timer = timer()
        timer.start()
        advanceTimeBy(4.minutes)

        timeouts.value = 2.minutes
        runCurrent()
        advanceTimeBy(2.minutes - 1.seconds)
        assertEquals(0, expired)

        advanceTimeBy(2.seconds)
        assertEquals(1, expired)
    }

    @Test
    fun `started again, it counts again`() = runTest {
        val timer = timer()
        timer.start()
        advanceTimeBy(6.minutes)

        timer.start()
        advanceTimeBy(6.minutes)

        assertEquals(2, expired)
    }

    @Test
    fun `going away with locking on leaving runs out at once`() = runTest {
        lockOnLeave.value = true
        val timer = timer()
        timer.start()

        timer.wentAway()

        assertEquals(1, expired)
    }

    @Test
    fun `going away without it does nothing, the count goes on`() = runTest {
        val timer = timer()
        timer.start()

        timer.wentAway()
        assertEquals(0, expired)

        advanceTimeBy(5.minutes + 1.seconds)
        assertEquals(1, expired)
    }

    @Test
    fun `going away with nothing to lock does nothing`() = runTest {
        lockOnLeave.value = true
        val timer = timer()

        timer.wentAway()

        assertEquals(0, expired)
    }

    @Test
    fun `going to a screen of the system is not leaving, until the app is back`() = runTest {
        lockOnLeave.value = true
        val timer = timer()
        timer.start()

        timer.leavingForSystemScreen()
        timer.wentAway()
        assertEquals(0, expired)

        timer.cameBack()
        timer.wentAway()
        assertEquals(1, expired)
    }

    private fun TestScope.timer() = ClockInactivityTimer(
        clock = { testScheduler.currentTime.milliseconds + slept },
        scope = backgroundScope,
        timeouts = timeouts,
        lockOnLeave = lockOnLeave,
    ).also {
        it.setOnExpired { expired++ }
        runCurrent()
    }
}
