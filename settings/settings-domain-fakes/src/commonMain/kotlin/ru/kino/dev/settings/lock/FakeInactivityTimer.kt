package ru.kino.dev.settings.lock

/** A timer the tests run out by hand ([expire]); it never runs out by itself. */
class FakeInactivityTimer : InactivityTimer {

    private var onExpired: (() -> Unit)? = null

    /** Whether there is something it counts for: started and neither stopped nor run out since. */
    var running: Boolean = false
        private set

    var touches: Int = 0
        private set

    /** How many times the app said it opens a screen of the system itself. */
    var systemScreens: Int = 0
        private set

    /** How many times the desktop said the system slept. */
    var wakings: Int = 0
        private set

    override fun setOnExpired(listener: (() -> Unit)?) {
        onExpired = listener
    }

    override fun start() {
        running = true
    }

    override fun stop() {
        running = false
    }

    override fun touched() {
        touches++
    }

    override fun cameBack() = Unit

    override fun wentAway() = Unit

    override fun leavingForSystemScreen() {
        systemScreens++
    }

    override fun wokeUp() {
        wakings++
    }

    /** The time ran out - told only while it is running, as the real one. */
    fun expire() {
        if (!running) return
        running = false
        onExpired?.invoke()
    }
}
