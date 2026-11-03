package ru.kino.dev.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PaneCloseTest {

    private val calls = mutableListOf<String>()
    private val pane = PaneClose()

    @Test
    fun `no handler - not handled`() {
        assertFalse(pane.close())
    }

    @Test
    fun `a handler registered answers`() {
        pane.register { calls += "form" }

        assertTrue(pane.close())
        assertEquals(listOf("form"), calls)
    }

    @Test
    fun `the last handler registered answers, alone`() {
        pane.register { calls += "form" }
        pane.register { calls += "dialog" }

        pane.close()

        assertEquals(listOf("dialog"), calls)
    }

    @Test
    fun `a handler unregistered - the one before answers`() {
        pane.register { calls += "form" }
        val unregister = pane.register { calls += "dialog" }

        unregister()
        pane.close()

        assertEquals(listOf("form"), calls)
    }

    @Test
    fun `every handler unregistered - not handled`() {
        val unregister = pane.register { calls += "form" }

        unregister()

        assertFalse(pane.close())
    }
}
