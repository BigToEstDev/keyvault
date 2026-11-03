package ru.kino.dev.vault.pres.testing

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.orbitmvi.orbit.OrbitContainerHost
import ru.kino.dev.log.ErrorLog
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.AppNavigator
import ru.kino.dev.navigation.Navigator

/*
 * What the tests of the processors of `vault` share: the log, the navigation. The database they save into is
 * `FakeVaultDatabase` of the test fixtures of vault-domain.
 */

const val DB = "content://docs/passwords.kdbx"

/** A navigator, and every event sent through it, in order. */
@OptIn(ExperimentalCoroutinesApi::class)
fun TestScope.recordNavigation(): Pair<Navigator, List<MviNavEvent>> {
    val navigator = AppNavigator()
    val events = mutableListOf<MviNavEvent>()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { navigator.events.collect { events += it } }
    return navigator to events
}

/**
 * Runs [actions] on the processor one after another, each to its end: what it starts is waited for - a search
 * waits its delay out, in virtual time. The methods of a processor give nothing to wait on (pass-docs,
 * `docs/app/mvi-arc.md`); the container knows what runs.
 */
suspend fun <H : OrbitContainerHost<*, *, *>> H.runEach(actions: Array<out H.() -> Unit>) {
    actions.forEach { action ->
        action()
        container.joinIntents()
    }
}

class RecordingLog : ErrorLog {
    val logged = mutableListOf<Throwable>()

    override fun unexpected(where: String, error: Throwable) {
        logged += error
    }
}
