package ru.kino.dev.desktop

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class WindowSizesTest {

    @Test
    fun `a large screen gets the size made for the desktop`() {
        assertEquals(DpSize(1000.dp, 720.dp), WindowSizes.initial(DpSize(1920.dp, 1040.dp)))
    }

    @Test
    fun `a smaller screen cuts the window to it`() {
        assertEquals(DpSize(900.dp, 700.dp), WindowSizes.initial(DpSize(900.dp, 700.dp)))
    }

    @Test
    fun `never under the least size`() {
        assertEquals(DpSize(800.dp, 600.dp), WindowSizes.initial(DpSize(800.dp, 560.dp)))
        assertEquals(WindowSizes.Min, WindowSizes.initial(DpSize(300.dp, 400.dp)))
    }
}
