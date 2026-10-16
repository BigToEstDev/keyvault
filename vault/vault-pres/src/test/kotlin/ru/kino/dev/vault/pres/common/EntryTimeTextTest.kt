package ru.kino.dev.vault.pres.common

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId

class EntryTimeTextTest {

    @Test
    fun `a time is written as day, month, year and hours and minutes, the same in every locale`() {
        assertEquals("02.01.2026 03:04", entryTimeText(1_767_323_045L, ZoneId.of("UTC")))
    }

    @Test
    fun `a time is written in the zone of the device`() {
        assertEquals("02.01.2026 06:04", entryTimeText(1_767_323_045L, ZoneId.of("Europe/Moscow")))
    }
}
