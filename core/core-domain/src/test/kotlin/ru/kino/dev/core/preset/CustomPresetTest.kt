package ru.kino.dev.core.preset

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.kino.dev.core.preset.StandardPreset.NOTES
import ru.kino.dev.core.preset.StandardPreset.PASSWORD
import ru.kino.dev.core.preset.StandardPreset.SERVICE
import ru.kino.dev.core.preset.StandardPreset.TITLE
import ru.kino.dev.core.preset.StandardPreset.URL
import ru.kino.dev.core.preset.StandardPreset.USER_NAME

class CustomPresetTest {

    private val id = PresetId("4f1c")

    @Test
    fun `the standard preset is the title and one card of the kdbx fields and the service`() {
        val preset = StandardPreset.preset

        assertTrue(preset.isStandard)
        assertEquals(listOf(TITLE, USER_NAME, PASSWORD, SERVICE, URL, NOTES), preset.fields.map { it.key })
        assertEquals(1, preset.cards.size)
    }

    @Test
    fun `a preset of one's own is the title, the user name and the password, its cards, the notes last`() {
        val preset = CustomPreset.of(id, "Card", listOf(listOf("PIN"), listOf("Number", "CVC")))

        assertFalse(preset.isStandard)
        assertEquals(
            listOf(listOf(USER_NAME, PASSWORD, "PIN"), listOf("Number", "CVC"), listOf(NOTES)),
            preset.cards.map { card -> card.fields.map { it.key } },
        )
        assertEquals(listOf(TITLE, USER_NAME, PASSWORD, "PIN", "Number", "CVC", NOTES), preset.fields.map { it.key })
    }

    @Test
    fun `the title, the user name and the password are required, the password protected`() {
        val preset = CustomPreset.of(id, "Card", listOf(listOf("PIN")))

        assertEquals(listOf(TITLE, USER_NAME, PASSWORD), preset.fields.filter { it.required }.map { it.key })
        assertEquals(listOf(PASSWORD), preset.fields.filter { it.protected }.map { it.key })
    }

    @Test
    fun `a field the person names is plain text written only when filled, the standard ones always`() {
        val preset = CustomPreset.of(id, "Card", listOf(listOf("PIN")))

        assertEquals(PresetField("PIN"), preset.fields.first { it.key == "PIN" })
        assertEquals(listOf(TITLE, USER_NAME, PASSWORD, NOTES), preset.fields.filter { it.writtenWhenEmpty }.map { it.key })
    }

    @Test
    fun `a preset is built back from the names it was built from`() {
        val cards = listOf(emptyList(), listOf("Number", "CVC"), listOf("Bank"))

        assertEquals(cards, CustomPreset.cards(CustomPreset.of(id, "Card", cards)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a key twice is no preset`() {
        CustomPreset.of(id, "Card", listOf(listOf("PIN"), listOf("PIN")))
    }

    @Test
    fun `names are checked against the preset ignoring case`() {
        val preset = CustomPreset.of(id, "Card", listOf(listOf("PIN")))

        assertTrue(preset.hasKey("pin"))
        assertTrue(preset.hasKey("password"))
        assertFalse(preset.hasKey(SERVICE))
    }
}
