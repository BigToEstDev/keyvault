package ru.kino.dev.vault.entry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.kino.dev.vault.entry.StandardPreset.NOTES
import ru.kino.dev.vault.entry.StandardPreset.PASSWORD
import ru.kino.dev.vault.entry.StandardPreset.SERVICE
import ru.kino.dev.vault.entry.StandardPreset.TITLE
import ru.kino.dev.vault.entry.StandardPreset.URL
import ru.kino.dev.vault.entry.StandardPreset.USER_NAME

class EntryPresetTest {

    private val preset = StandardPreset.preset

    @Test
    fun `a foreign entry is laid over the preset, its own fields after it in the order of the file`() {
        val content = preset.layout(
            listOf(
                field(NOTES, "notes"),
                field("PIN", "1234", protected = true),
                field(PASSWORD, "secret", protected = true),
                field(TITLE, "Bank"),
                field("Card", "4111"),
                field(USER_NAME, "anton"),
            ),
        )

        assertEquals(listOf(TITLE, USER_NAME, PASSWORD, SERVICE, URL, NOTES), content.presetFields.map { it.key })
        assertEquals("Bank", content.value(TITLE))
        assertEquals("", content.value(SERVICE))
        assertEquals("", content.value(URL))
        assertEquals(listOf("PIN", "Card"), content.ownFields.map { it.key })
        assertTrue("protection comes from the file", content.ownFields[0].protected)
    }

    @Test
    fun `a second field under a key of the preset is an own field`() {
        val content = preset.layout(listOf(field(PASSWORD, "first"), field(PASSWORD, "second")))

        assertEquals("first", content.value(PASSWORD))
        assertEquals(listOf(field(PASSWORD, "second")), content.ownFields)
    }

    @Test
    fun `a key of the preset is found as the file spells it`() {
        val content = preset.layout(listOf(field("password", "lower")))

        assertEquals("", content.value(PASSWORD))
        assertEquals(listOf("password"), content.ownFields.map { it.key })
    }

    @Test
    fun `a new entry has every field of the preset empty, the password protected`() {
        val content = preset.blank()

        assertEquals(preset.fields.map { it.key }, content.presetFields.map { it.key })
        assertTrue(content.presetFields.all { it.value.isEmpty() })
        assertEquals(listOf(PASSWORD), content.presetFields.filter { it.protected }.map { it.key })
        assertEquals(emptyList<EntryField>(), content.ownFields)
    }

    @Test
    fun `the fields go out in the order of the form, an empty service left out`() {
        val content = preset.blank()
            .withValue(TITLE, "Mail")
            .withValue(PASSWORD, "secret")
            .copy(ownFields = listOf(field("PIN", "1")))

        assertEquals(listOf(TITLE, USER_NAME, PASSWORD, URL, NOTES, "PIN"), content.fields().map { it.key })
        assertTrue(content.fields().first { it.key == PASSWORD }.protected)
    }

    @Test
    fun `a filled service goes out`() {
        val content = preset.blank().withValue(SERVICE, "Mail")

        assertEquals("Mail", content.fields().first { it.key == SERVICE }.value)
    }

    @Test
    fun `nothing of a foreign entry is lost on the way through`() {
        val read = listOf(
            field("PIN", "1234", protected = true),
            field(TITLE, "Bank"),
            field(USER_NAME, "anton"),
            field(PASSWORD, "secret", protected = true),
            field(URL, "", protected = true),
            field(NOTES, "notes"),
            field(SERVICE, "Bank app"),
            field(PASSWORD, "old"),
        )

        val written = preset.layout(read).fields()

        assertEquals(read.sortedBy { it.key + it.value }, written.sortedBy { it.key + it.value })
    }

    @Test
    fun `own field names are checked against the preset ignoring case`() {
        assertTrue(preset.hasKey("password"))
        assertTrue(preset.hasKey(" URL ".trim()))
        assertFalse(preset.hasKey("PIN"))
    }

    private fun field(key: String, value: String, protected: Boolean = false) = EntryField(key, value, protected)
}
