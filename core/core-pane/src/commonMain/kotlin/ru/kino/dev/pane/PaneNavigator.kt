package ru.kino.dev.pane

import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.Navigator

/**
 * What a container does with the navigation of one of its panes: true - taken, handled by the container (open a tab,
 * close it); false - not its, goes on to the navigation around the container.
 */
fun interface PaneRouter {
    fun route(event: MviNavEvent): Boolean
}

/**
 * The navigator of one pane of a container: what [router] takes stays in the container, the rest goes to [parent].
 * One for each pane - the container knows which pane an event came from.
 */
class PaneNavigator(private val parent: Navigator, private val router: PaneRouter) : Navigator {

    override suspend fun navigate(event: MviNavEvent) {
        if (!router.route(event)) parent.navigate(event)
    }
}
