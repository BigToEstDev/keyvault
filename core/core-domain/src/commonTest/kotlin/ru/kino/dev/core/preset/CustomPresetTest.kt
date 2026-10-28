package ru.kino.dev.core.preset

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
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

    @Test
    fun `the fields the person made protected are protected, and are built back`() {
        val preset = CustomPreset.of(id, "Card", listOf(listOf("PIN"), listOf("Number", "CVC")), setOf("PIN", "CVC"))

        assertEquals(listOf(PASSWORD, "PIN", "CVC"), preset.fields.filter { it.protected }.map { it.key })
        assertEquals(setOf("PIN", "CVC"), CustomPreset.protectedKeys(preset))
    }

    @Test
    fun `a protected name in no card is nothing`() {
        val preset = CustomPreset.of(id, "Card", listOf(listOf("PIN")), setOf("CVC"))

        assertEquals(emptySet<String>(), CustomPreset.protectedKeys(preset))
    }

    @Test
    fun `the types of the fields are in the preset, and are built back - a name in no card is nothing`() {
        val preset = CustomPreset.of(
            id,
            "Card",
            listOf(listOf("PIN"), listOf("Number")),
            fieldTypes = mapOf("Number" to "card", "CVC" to "card"),
        )

        assertEquals("card", preset.fields.first { it.key == "Number" }.type)
        assertEquals(null, preset.fields.first { it.key == "PIN" }.type)
        assertEquals(mapOf("Number" to "card"), CustomPreset.fieldTypes(preset))
    }

    @Test
    fun `a key twice is no preset`() {
        assertFailsWith<IllegalArgumentException> {
            CustomPreset.of(id, "Card", listOf(listOf("PIN"), listOf("PIN")))
        }
    }

    @Test
    fun `names are checked against the preset ignoring case`() {
        val preset = CustomPreset.of(id, "Card", listOf(listOf("PIN")))

        assertTrue(preset.hasKey("pin"))
        assertTrue(preset.hasKey("password"))
        assertFalse(preset.hasKey(SERVICE))
    }
}
