package ru.kino.dev.settings.lock

import kotlin.time.Duration

/** The time since some fixed point, counting while the phone sleeps - so a lock is due after a night. */
fun interface ElapsedClock {
    fun now(): Duration
}

/**
 * Counts the time nobody touches the app and says when it ran out - for as long as there is something to lock
 * (pass-docs, Step 59). How long is the setting of `Auto-lock`; a new one counts from the moment it is chosen.
 *
 * The app tells it what it sees - touches, coming back; `vault` starts it with an open database and listens.
 * One rule: put away, the screen off, another app on top - no touches all the same, and the same time runs
 * out. A process frozen in the background sleeps through the count - so coming back checks the clock.
 */
interface InactivityTimer {

    /**
     * Who is told the time ran out - once for each [start], on the thread that saw it: from [cameBack] it is
     * told before the call returns, so the app can cover its screens before they are drawn.
     */
    fun setOnExpired(listener: (() -> Unit)?)

    /** There is something to lock: the count starts now. */
    fun start()

    /** Nothing to lock any more: the count stops. */
    fun stop()

    /** A touch or a key - the person is here. */
    fun touched()

    /** The app is on the screen again: when the count was slept through, the time ran out now. */
    fun cameBack()
}
