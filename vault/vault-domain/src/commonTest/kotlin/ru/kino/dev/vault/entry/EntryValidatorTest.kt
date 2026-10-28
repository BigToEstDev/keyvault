package ru.kino.dev.vault.entry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import ru.kino.dev.core.preset.StandardPreset
import ru.kino.dev.core.preset.StandardPreset.PASSWORD
import ru.kino.dev.core.preset.StandardPreset.TITLE
import ru.kino.dev.core.preset.StandardPreset.USER_NAME

class EntryValidatorTest {

    private val preset = StandardPreset.preset

    private val filled = preset.blank()
        .withValue(TITLE, "Mail")
        .withValue(USER_NAME, "anton")
        .withValue(PASSWORD, "secret")

    @Test
    fun `a new entry needs every required field of the preset`() {
        assertEquals(
            listOf(FieldProblem.Required(TITLE), FieldProblem.Required(USER_NAME), FieldProblem.Required(PASSWORD)),
            EntryValidator.problems(preset.blank(), read = null),
        )
        assertEquals(emptyList<FieldProblem>(), EntryValidator.problems(filled, read = null))
    }

    @Test
    fun `a password of spaces is filled`() {
        assertEquals(emptyList<FieldProblem>(), EntryValidator.problems(filled.withValue(PASSWORD, "   "), read = null))
    }

    @Test
    fun `a required field cannot be emptied`() {
        assertEquals(
            listOf(FieldProblem.Required(PASSWORD)),
            EntryValidator.problems(filled.withValue(PASSWORD, ""), read = filled),
        )
    }

    @Test
    fun `a required field empty in the file already is not asked for`() {
        val foreign = filled.withValue(USER_NAME, "")

        assertEquals(
            emptyList<FieldProblem>(),
            EntryValidator.problems(foreign.withValue(TITLE, "Bank"), read = foreign),
        )
    }

    @Test
    fun `the name of an own field is not empty and not taken, ignoring case and spaces`() {
        val content = filled.copy(ownFields = listOf(field("PIN")))

        assertEquals(NameProblem.EMPTY, EntryValidator.nameProblem(content, "   "))
        assertEquals(NameProblem.TAKEN, EntryValidator.nameProblem(content, " password "))
        assertEquals(NameProblem.TAKEN, EntryValidator.nameProblem(content, "pin"))
        assertNull(EntryValidator.nameProblem(content, "Card"))
        assertNull(EntryValidator.nameProblem(content, "pin", renamedIndex = 0), "its own name is not a rival")
        assertEquals(NameProblem.TAKEN, EntryValidator.nameProblem(content, "keyvaultpreset"), "the link to a preset")
    }

    @Test
    fun `a name written here follows the rules`() {
        val read = filled.copy(ownFields = listOf(field("PIN")))
        val edited = read.copy(ownFields = read.ownFields + field("pin"))

        assertEquals(listOf(FieldProblem.BadName(1, NameProblem.TAKEN)), EntryValidator.problems(edited, read))
    }

    @Test
    fun `two fields of another program under one key - the second is to be renamed`() {
        val read = filled.copy(ownFields = listOf(field("PIN"), field("PIN")))

        assertEquals(listOf(FieldProblem.BadName(1, NameProblem.TAKEN)), EntryValidator.problems(read, read))

        val renamed = read.copy(ownFields = listOf(field("PIN"), field("PIN 2")))
        assertEquals(emptyList<FieldProblem>(), EntryValidator.problems(renamed, read))
    }

    @Test
    fun `a second field under a key of the preset is to be renamed`() {
        val read = preset.layout(filled.fields() + field(PASSWORD))

        assertEquals(listOf(FieldProblem.BadName(0, NameProblem.TAKEN)), EntryValidator.problems(read, read))
    }

    @Test
    fun `a name of another program the core takes is left as it is`() {
        val read = filled.copy(ownFields = listOf(field("password"), field("Pin"), field("PIN")))

        assertEquals(emptyList<FieldProblem>(), EntryValidator.problems(read, read))
    }

    @Test
    fun `every problem comes at once`() {
        val edited = preset.blank().withValue(TITLE, "Mail").copy(ownFields = listOf(field(" "), field("url")))

        assertEquals(
            listOf(
                FieldProblem.Required(USER_NAME),
                FieldProblem.Required(PASSWORD),
                FieldProblem.BadName(0, NameProblem.EMPTY),
                FieldProblem.BadName(1, NameProblem.TAKEN),
            ),
            EntryValidator.problems(edited, read = null),
        )
    }

    @Test
    fun `up to 12 own fields can be added, more read are kept but no more added`() {
        fun owning(count: Int) = filled.copy(ownFields = (1..count).map { field("F$it") })

        assertTrue(EntryValidator.canAddField(owning(11)))
        assertFalse(EntryValidator.canAddField(owning(12)))
        assertFalse(EntryValidator.canAddField(owning(15)))
        assertEquals(emptyList<FieldProblem>(), EntryValidator.problems(owning(15), read = owning(15)))
    }

    private fun field(key: String, value: String = "v") = EntryField(key, value, protected = false)
}
