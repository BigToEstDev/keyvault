package ru.kino.dev.vault.group

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GroupNameValidatorTest {

    private val tree = sampleTree()

    @Test
    fun `a new name in its group is fine`() {
        assertNull(GroupNameValidator.problem(tree, ROOT, "Bank"))
    }

    @Test
    fun `spaces around the name are not stored`() {
        assertEquals("Bank", GroupNameValidator.normalize("  Bank \t"))
    }

    @Test
    fun `a name of spaces is empty`() {
        assertTrue(GroupNameValidator.problem(tree, ROOT, "   ") is GroupFailure.NameEmpty)
    }

    @Test
    fun `the longest name is allowed, one more character is not`() {
        val longest = "a".repeat(GroupNameValidator.MAX_LENGTH)

        assertNull(GroupNameValidator.problem(tree, ROOT, longest))
        assertTrue(GroupNameValidator.problem(tree, ROOT, longest + "a") is GroupFailure.NameTooLong)
    }

    @Test
    fun `the length counts characters, not utf-16 units`() {
        // An emoji is two utf-16 units and one character
        val emojis = "🔑".repeat(GroupNameValidator.MAX_LENGTH)

        assertNull(GroupNameValidator.problem(tree, ROOT, emojis))
    }

    @Test
    fun `spaces around do not count towards the length`() {
        val longest = "a".repeat(GroupNameValidator.MAX_LENGTH)

        assertNull(GroupNameValidator.problem(tree, ROOT, "  $longest  "))
    }

    @Test
    fun `a neighbour with the same name takes it`() {
        assertTrue(GroupNameValidator.problem(tree, ROOT, "work") is GroupFailure.NameTaken)
    }

    @Test
    fun `renaming a group to its own name in another case is fine`() {
        assertNull(GroupNameValidator.problem(tree, ROOT, "WORK", renamedId = WORK))
    }
}
