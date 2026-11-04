package ru.kino.dev.pane

import ru.kino.dev.navigation.MviNavEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PaneRoutersTest {

    private val routers = PaneRouters()

    /** Which pane's router got each event. */
    private val routed = mutableListOf<String>()

    private fun recording(name: String) = PaneRouter { routed += name; true }

    @Test
    fun `a pane navigates through the router it was drawn with`() {
        val first = routers.router("first")
        routers.set("first", recording("first"))
        routers.set("second", recording("second"))

        first.route(MviNavEvent.NavigateBack)

        assertEquals(listOf("first"), routed)
    }

    @Test
    fun `drawn again with another router - that one, through the navigator it was given before`() {
        val first = routers.router("first")
        routers.set("first", recording("old"))
        routers.set("first", recording("new"))

        first.route(MviNavEvent.NavigateBack)

        assertEquals(listOf("new"), routed)
    }

    @Test
    fun `another pane drawn since does not take its navigation`() {
        val first = routers.router("first")
        routers.set("first", recording("first"))
        // The same place in the layout draws another pane now
        routers.set("second", recording("second"))
        routers.set("third", recording("third"))

        first.route(MviNavEvent.NavigateBack)

        assertEquals(listOf("first"), routed)
    }

    @Test
    fun `a pane gone - its navigation is dropped, not passed on`() {
        val first = routers.router("first")
        routers.set("first", recording("first"))
        routers.retain(setOf("second"))

        assertTrue(first.route(MviNavEvent.NavigateBack))
        assertEquals(emptyList(), routed)
    }
}
