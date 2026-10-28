package ru.kino.dev.welcome.pres

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.Navigator

/**
 * A real [Navigator] and the events sent through it. Subscribed before the test acts: the navigator keeps
 * no event for a subscriber that comes later.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun TestScope.recordNavigation(): Pair<Navigator, List<MviNavEvent>> {
    val navigator = Navigator()
    val events = mutableListOf<MviNavEvent>()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
        navigator.events.collect { events += it }
    }
    return navigator to events
}
