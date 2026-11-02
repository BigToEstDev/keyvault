package ru.kino.dev.settings.pres.testing

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.orbitmvi.orbit.OrbitContainerHost
import ru.kino.dev.log.ErrorLog
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.Navigator

/* What the tests of the processors of `settings` share: the log, the navigation. */

/** A navigator, and every event sent through it, in order. */
@OptIn(ExperimentalCoroutinesApi::class)
fun TestScope.recordNavigation(): Pair<Navigator, List<MviNavEvent>> {
    val navigator = Navigator()
    val events = mutableListOf<MviNavEvent>()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { navigator.events.collect { events += it } }
    return navigator to events
}

/**
 * Runs [actions] on the processor one after another, each to its end: what it starts is waited for. The methods of a
 * processor give nothing to wait on (pass-docs, `docs/app/mvi-arc.md`); the container knows what runs.
 */
suspend fun <H : OrbitContainerHost<*, *, *>> H.runEach(actions: Array<out H.() -> Unit>) {
    actions.forEach { runToEnd(it) }
}

/** Runs [action] on the processor to its end - [runEach] of one. */
suspend fun <H : OrbitContainerHost<*, *, *>> H.runToEnd(action: H.() -> Unit) {
    action()
    container.joinIntents()
}

class RecordingLog : ErrorLog {
    val logged = mutableListOf<Throwable>()

    override fun unexpected(where: String, error: Throwable) {
        logged += error
    }
}
