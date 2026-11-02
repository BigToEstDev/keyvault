package ru.kino.dev.ui.theme

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class LayoutTypeTest {

    @Test
    fun `a phone is mobile`() {
        assertEquals(LayoutType.Mobile, LayoutType.of(411.dp))
    }

    @Test
    fun `just short of the line is mobile`() {
        assertEquals(LayoutType.Mobile, LayoutType.of(719.dp))
    }

    @Test
    fun `from the line on is tablet`() {
        assertEquals(LayoutType.Tablet, LayoutType.of(720.dp))
        assertEquals(LayoutType.Tablet, LayoutType.of(1000.dp))
    }
}
