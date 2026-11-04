package ru.kino.dev.pane

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TabClosingTest {

    private val tabs = listOf("a", "b", "c")

    @Test
    fun `the tab shown closed - the next one is shown`() {
        assertEquals(ShownTabs(listOf("a", "c"), "c"), closeTab(tabs, active = "b", key = "b") { it })
    }

    @Test
    fun `the last tab shown closed - the one before is shown`() {
        assertEquals(ShownTabs(listOf("a", "b"), "b"), closeTab(tabs, active = "c", key = "c") { it })
    }

    @Test
    fun `a tab not shown closed - the shown one stays`() {
        assertEquals(ShownTabs(listOf("b", "c"), "c"), closeTab(tabs, active = "c", key = "a") { it })
    }

    @Test
    fun `the only tab closed - none is shown`() {
        assertEquals(ShownTabs(emptyList(), null), closeTab(listOf("a"), active = "a", key = "a") { it })
    }

    @Test
    fun `a tab not open - nothing to close`() {
        assertNull(closeTab(tabs, active = "a", key = "d") { it })
    }
}
