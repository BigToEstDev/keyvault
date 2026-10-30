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
 * The app tells it what it sees - touches, going away, coming back; `vault` starts it with an open database
 * and listens. Going away runs it out at once when `Lock on leaving the app` is on.
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

    /**
     * The app went off the screen - put away, the screen off. With `Lock on leaving the app` on, the time ran
     * out now; off - the count goes on, as with the screen on. Not for a screen of the app being built anew.
     */
    fun wentAway()

    /**
     * The app itself is about to open a screen of the system - a file to choose, the prompt of biometrics: the
     * next [wentAway] is not leaving, until the app is back ([cameBack]). The count goes on meanwhile.
     */
    fun leavingForSystemScreen()

    /**
     * The system slept - the desktop sees it after waking (pass-docs, Step 89): the time ran out now, whatever
     * `Lock on leaving the app` says and however long the timeout is.
     */
    fun wokeUp()
}
