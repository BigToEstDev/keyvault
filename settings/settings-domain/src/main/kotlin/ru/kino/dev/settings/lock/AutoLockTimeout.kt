package ru.kino.dev.settings.lock

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * How long the app may lie untouched before the open database locks - the choices of `Auto-lock`
 * (pass-docs, `settings/settings.md`, "Lock and sign-in").
 */
enum class AutoLockTimeout(val duration: Duration) {
    Seconds30(30.seconds),
    Minute1(1.minutes),
    Minutes2(2.minutes),
    Minutes5(5.minutes),
    Minutes10(10.minutes),
    Minutes15(15.minutes),
    Minutes30(30.minutes),
    ;

    companion object {
        /** Until one is chosen - the one of Step 46. */
        val Default = Minutes5
    }
}
