package ru.kino.dev.core

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.kino.dev.core.preset.StandardPreset

class FieldProtectionTest {

    @Test
    fun `each standard field is private by its own flag`() {
        val protection = FieldProtection(title = true, userName = false, password = false, url = true, notes = false)

        assertEquals(
            listOf(true, false, false, true, false),
            listOf(
                StandardPreset.TITLE,
                StandardPreset.USER_NAME,
                StandardPreset.PASSWORD,
                StandardPreset.URL,
                StandardPreset.NOTES,
            ).map(protection::protects),
        )
    }

    @Test
    fun `a field of one's own is never private by the database`() {
        val all = FieldProtection(title = true, userName = true, password = true, url = true, notes = true)

        assertEquals(false, all.protects(StandardPreset.SERVICE))
        assertEquals(false, all.protects("PIN"))
    }
}
