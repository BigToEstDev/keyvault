package ru.kino.dev.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PaneBackTest {

    private val calls = mutableListOf<String>()
    private val back = PaneBack()

    @Test
    fun `no handler - not handled`() {
        assertFalse(back.handle())
    }

    @Test
    fun `a handler registered answers`() {
        back.register { calls += "form" }

        assertTrue(back.handle())
        assertEquals(listOf("form"), calls)
    }

    @Test
    fun `the last handler registered answers, alone`() {
        back.register { calls += "form" }
        back.register { calls += "dialog" }

        back.handle()

        assertEquals(listOf("dialog"), calls)
    }

    @Test
    fun `a handler unregistered - the one before answers`() {
        back.register { calls += "form" }
        val unregister = back.register { calls += "dialog" }

        unregister()
        back.handle()

        assertEquals(listOf("form"), calls)
    }

    @Test
    fun `every handler unregistered - not handled`() {
        val unregister = back.register { calls += "form" }

        unregister()

        assertFalse(back.handle())
    }
}
