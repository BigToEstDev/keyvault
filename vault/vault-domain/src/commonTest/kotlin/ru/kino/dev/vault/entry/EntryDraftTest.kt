package ru.kino.dev.vault.entry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import ru.kino.dev.core.preset.CustomPreset
import ru.kino.dev.core.preset.PresetId
import ru.kino.dev.core.preset.StandardPreset
import ru.kino.dev.core.preset.StandardPreset.PASSWORD
import ru.kino.dev.core.preset.StandardPreset.SERVICE
import ru.kino.dev.core.preset.StandardPreset.TITLE
import ru.kino.dev.core.preset.StandardPreset.URL
import ru.kino.dev.core.preset.StandardPreset.USER_NAME

class EntryDraftTest {

    private val standard = StandardPreset.preset
    private val card = CustomPreset.of(PresetId("card"), "Card", listOf(listOf("PIN"), listOf("Number")))

    private val filled = EntryDraft(
        standard.blank()
            .withValue(TITLE, "Visa")
            .withValue(USER_NAME, "anton")
            .withValue(PASSWORD, "secret")
            .withValue(URL, "bank.ru"),
    )

    @Test
    fun `the fields both presets have keep their values`() {
        val content = filled.withPreset(card).content

        assertEquals(card, content.preset)
        assertEquals("Visa", content.value(TITLE))
        assertEquals("anton", content.value(USER_NAME))
        assertEquals("secret", content.value(PASSWORD))
        assertEquals("", content.value("PIN"))
    }

    @Test
    fun `a filled field the new preset lacks is hidden, an empty one dropped`() {
        val draft = filled.withPreset(card)

        assertEquals(listOf(EntryField(URL, "bank.ru", protected = false)), draft.hidden)
        assertFalse(draft.hidden.any { it.key == SERVICE }, "the empty service is not kept")
        assertTrue(draft.losesHidden)
    }

    @Test
    fun `back to the preset before, the hidden values are where they were`() {
        val there = filled.withPreset(card)
        val back = there.copy(content = there.content.withValue("PIN", "1234")).withPreset(standard)

        assertEquals("bank.ru", back.content.value(URL))
        assertEquals(listOf(EntryField("PIN", "1234", protected = false)), back.hidden)
        assertEquals("1234", back.withPreset(card).content.value("PIN"))
    }

    @Test
    fun `own fields stay own, one under a key of the new preset moves into it`() {
        val draft = filled.copy(
            content = filled.content.copy(ownFields = listOf(EntryField("PIN", "1234", true), EntryField("Bank", "Sber", false))),
        ).withPreset(card)

        assertEquals("1234", draft.content.value("PIN"))
        assertTrue(draft.content.presetFields.first { it.key == "PIN" }.protected, "protection comes with the value")
        assertEquals(listOf("Bank"), draft.content.ownFields.map { it.key })
    }

    @Test
    fun `a value shown wins over a hidden one under the same key`() {
        val draft = EntryDraft(
            card.blank().withValue("PIN", "new"),
            hidden = listOf(EntryField("PIN", "old", protected = false)),
        ).withPreset(standard)

        assertEquals(listOf(EntryField("PIN", "new", protected = false)), draft.hidden)
    }

    @Test
    fun `hidden fields kept after all are own fields, last, and nothing is hidden any more`() {
        val draft = filled.copy(content = filled.content.copy(ownFields = listOf(EntryField("Bank", "Sber", false))))
            .withPreset(card)
            .withHiddenAsOwn()

        assertEquals(listOf("Bank", URL), draft.content.ownFields.map { it.key })
        assertEquals("bank.ru", draft.content.ownFields.last().value)
        assertFalse(draft.losesHidden)
    }

    @Test
    fun `nothing hidden, nothing lost`() {
        assertFalse(filled.withPreset(standard).losesHidden)
    }

    @Test
    fun `an entry read with one preset and saved with another is checked against what it held`() {
        val read = card.blank().withValue(TITLE, "Visa").withValue(USER_NAME, "anton").withValue(PASSWORD, "secret")
        val edited = EntryDraft(read).withPreset(standard).content

        assertEquals(emptyList<FieldProblem>(), EntryValidator.problems(edited, read))
        assertEquals(
            listOf(FieldProblem.Required(USER_NAME)),
            EntryValidator.problems(edited.withValue(USER_NAME, ""), read),
        )
    }
}
