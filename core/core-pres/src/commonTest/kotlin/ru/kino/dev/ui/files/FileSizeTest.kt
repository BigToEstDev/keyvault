package ru.kino.dev.ui.files

import kotlin.test.Test
import kotlin.test.assertEquals

class FileSizeTest {

    @Test
    fun `a whole number of units has no digit after the point`() {
        assertEquals("2", oneDigit(2L * 1024 * 1024, 1024L * 1024, ","))
    }

    @Test
    fun `one digit after the separator of the language`() {
        assertEquals("1,5", oneDigit(1536L * 1024, 1024L * 1024, ","))
        assertEquals("1.5", oneDigit(1536L * 1024, 1024L * 1024, "."))
    }

    @Test
    fun `rounded to the nearest tenth`() {
        assertEquals("340", oneDigit(340L * 1024 + 20, 1024L, "."))
        assertEquals("1.3", oneDigit(1331L, 1024L, "."))
    }
}
