package ru.kino.dev.core.preset

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PresetValidatorTest {

    private val standardName = "Стандарт"
    private val card = CustomPreset.of(PresetId("1"), "Card", listOf(listOf("PIN")))

    @Test
    fun `a free name is fine, the spaces around it aside`() {
        assertNull(PresetValidator.nameProblem("  Wi-Fi ", standardName, listOf(card)))
    }

    @Test
    fun `a name of nothing but spaces is empty`() {
        assertEquals(PresetNameProblem.EMPTY, PresetValidator.nameProblem("   ", standardName, emptyList()))
    }

    @Test
    fun `a name is at most 64 characters, an emoji counted as one`() {
        val longest = "🔑".repeat(PresetLimits.MAX_NAME_LENGTH)

        assertNull(PresetValidator.nameProblem(longest, standardName, emptyList()))
        assertEquals(PresetNameProblem.TOO_LONG, PresetValidator.nameProblem(longest + "a", standardName, emptyList()))
    }

    @Test
    fun `the name of another preset is taken, ignoring case`() {
        assertEquals(PresetNameProblem.TAKEN, PresetValidator.nameProblem("card", standardName, listOf(card)))
    }

    @Test
    fun `the name of the standard preset is taken, ignoring case`() {
        assertEquals(PresetNameProblem.TAKEN, PresetValidator.nameProblem("стандарт", standardName, emptyList()))
    }

    @Test
    fun `a preset changed keeps its own name`() {
        assertNull(PresetValidator.nameProblem("Card", standardName, listOf(card), editedId = card.id))
    }

    @Test
    fun `a free field name is fine`() {
        assertNull(PresetValidator.fieldNameProblem(listOf(listOf("PIN")), "CVC"))
    }

    @Test
    fun `a field name of nothing but spaces is empty`() {
        assertEquals(FieldNameProblem.EMPTY, PresetValidator.fieldNameProblem(listOf(emptyList()), " "))
    }

    @Test
    fun `the fields every preset has and the link are taken, ignoring case`() {
        listOf("Title", "username", "PASSWORD", "notes", "keyvaultpreset").forEach {
            assertEquals(FieldNameProblem.TAKEN, PresetValidator.fieldNameProblem(listOf(emptyList()), it), it)
        }
    }

    @Test
    fun `the service and the url are free - plain fields of a preset of one's own`() {
        assertNull(PresetValidator.fieldNameProblem(listOf(emptyList()), "URL"))
        assertNull(PresetValidator.fieldNameProblem(listOf(emptyList()), "Service"))
    }

    @Test
    fun `a field name of another card is taken, ignoring case`() {
        assertEquals(FieldNameProblem.TAKEN, PresetValidator.fieldNameProblem(listOf(listOf("PIN"), listOf("CVC")), "cvc"))
    }

    @Test
    fun `a field renamed keeps its own name`() {
        val cards = listOf(listOf("PIN"), listOf("CVC"))

        assertNull(PresetValidator.fieldNameProblem(cards, "cvc", renamed = FieldPosition(1, 0)))
    }

    @Test
    fun `nine presets of one's own and the standard one are the most`() {
        val nine = (1..9).map { CustomPreset.of(PresetId("$it"), "P$it", listOf(emptyList())) }

        assertTrue(PresetValidator.canAdd(nine.take(8)))
        assertFalse(PresetValidator.canAdd(nine))
        assertEquals(listOf(PresetProblem.TooMany), PresetValidator.problems("New", listOf(emptyList()), standardName, nine))
        assertEquals(emptyList<PresetProblem>(), PresetValidator.problems("P1", listOf(emptyList()), standardName, nine, nine[0].id))
    }

    @Test
    fun `every problem of a preset is told, with where it is`() {
        val problems = PresetValidator.problems(
            name = "Card",
            cards = listOf(listOf("PIN", ""), listOf("pin", "CVC")),
            standardName = standardName,
            presets = listOf(card),
        )

        assertEquals(
            listOf(
                PresetProblem.Name(PresetNameProblem.TAKEN),
                PresetProblem.FieldName(FieldPosition(0, 0), FieldNameProblem.TAKEN),
                PresetProblem.FieldName(FieldPosition(0, 1), FieldNameProblem.EMPTY),
                PresetProblem.FieldName(FieldPosition(1, 0), FieldNameProblem.TAKEN),
            ),
            problems,
        )
    }

    @Test
    fun `a card takes up to 12 fields, the user name and the password of the first one counted`() {
        val names = (1..12).map { "F$it" }

        assertTrue(PresetValidator.canAddField(listOf(names.take(9)), card = 0))
        assertFalse(PresetValidator.canAddField(listOf(names.take(10)), card = 0))
        assertTrue(PresetValidator.canAddField(listOf(emptyList(), names.take(11)), card = 1))
        assertFalse(PresetValidator.canAddField(listOf(emptyList(), names), card = 1))
    }

    @Test
    fun `a preset takes up to 6 cards, the first one counted`() {
        assertTrue(PresetValidator.canAddCard(List(5) { emptyList() }))
        assertFalse(PresetValidator.canAddCard(List(6) { emptyList() }))
    }

    @Test
    fun `no field goes into a card that is not there`() {
        assertFalse(PresetValidator.canAddField(listOf(emptyList()), card = 1))
    }
}
