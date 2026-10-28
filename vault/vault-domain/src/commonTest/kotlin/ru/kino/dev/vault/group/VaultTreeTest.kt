package ru.kino.dev.vault.group

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VaultTreeTest {

    private val tree = sampleTree()

    @Test
    fun `the groups inside a group come in the order of the database, without the recycle bin`() {
        assertEquals(listOf(WORK, HOME), tree.subgroups(ROOT).map { it.id })
        assertEquals(listOf(MAIL), tree.subgroups(WORK).map { it.id })
    }

    @Test
    fun `the recycle bin and the groups inside it are not there`() {
        assertNull(tree.group(BIN))
        assertNull(tree.group(OLD))
        assertEquals(emptyList<VaultGroup>(), tree.subgroups(BIN))
    }

    @Test
    fun `a group counts its own entries and those of every group below it`() {
        assertEquals(EntryCount(direct = 2, nested = 5), tree.entryCount(WORK))
        assertEquals(EntryCount(direct = 3, nested = 3), tree.entryCount(MAIL))
        assertEquals(EntryCount(direct = 0, nested = 0), tree.entryCount(HOME))
    }

    @Test
    fun `the root counts the whole database except the recycle bin`() {
        assertEquals(EntryCount(direct = 1, nested = 6), tree.entryCount(ROOT))
    }

    @Test
    fun `a group that is not there counts nothing`() {
        assertEquals(EntryCount.None, tree.entryCount(OLD))
        assertEquals(EntryCount.None, tree.entryCount(GroupId("missing")))
    }

    @Test
    fun `the path goes from the root down to the group`() {
        assertEquals(listOf(ROOT, WORK, MAIL), tree.path(MAIL).map { it.id })
        assertEquals(listOf(ROOT), tree.path(ROOT).map { it.id })
    }

    @Test
    fun `there is no path into the recycle bin`() {
        assertEquals(emptyList<VaultGroup>(), tree.path(OLD))
    }

    @Test
    fun `a name is taken by a neighbour, ignoring case and spaces around it`() {
        assertTrue(tree.isNameTaken(ROOT, "work"))
        assertTrue(tree.isNameTaken(ROOT, "  HOME "))
    }

    @Test
    fun `the same name in another group is not taken`() {
        assertFalse(tree.isNameTaken(WORK, "Home"))
    }

    @Test
    fun `the hidden recycle bin does not take its name`() {
        assertFalse(tree.isNameTaken(ROOT, "Recycle Bin"))
    }

    @Test
    fun `a group being renamed is not its own rival`() {
        assertFalse(tree.isNameTaken(ROOT, "WORK", renamedId = WORK))
        assertTrue(tree.isNameTaken(ROOT, "Home", renamedId = WORK))
    }

    @Test
    fun `the groups below a group come at any depth, each after its parent, without the recycle bin`() {
        assertEquals(listOf(WORK, MAIL, HOME), tree.descendants(ROOT).map { it.id })
        assertEquals(listOf(MAIL), tree.descendants(WORK).map { it.id })
        assertEquals(emptyList<VaultGroup>(), tree.descendants(HOME))
        assertEquals(emptyList<VaultGroup>(), tree.descendants(BIN))
    }

    @Test
    fun `a group is within itself and every group above it, never within the recycle bin`() {
        assertTrue(tree.isWithin(MAIL, MAIL))
        assertTrue(tree.isWithin(MAIL, WORK))
        assertTrue(tree.isWithin(MAIL, ROOT))
        assertFalse(tree.isWithin(WORK, MAIL))
        assertFalse(tree.isWithin(HOME, WORK))
        assertFalse(tree.isWithin(OLD, ROOT))
        assertFalse(tree.isWithin(OLD, BIN))
    }

    @Test
    fun `the path from a group down to another starts at the first and ends at the second`() {
        assertEquals(listOf(WORK, MAIL), tree.pathFrom(WORK, MAIL).map { it.id })
        assertEquals(listOf(ROOT, WORK, MAIL), tree.pathFrom(ROOT, MAIL).map { it.id })
        assertEquals(listOf(WORK), tree.pathFrom(WORK, WORK).map { it.id })
    }

    @Test
    fun `there is no path from a group to one that is not below it`() {
        assertEquals(emptyList<VaultGroup>(), tree.pathFrom(MAIL, WORK))
        assertEquals(emptyList<VaultGroup>(), tree.pathFrom(WORK, HOME))
        assertEquals(emptyList<VaultGroup>(), tree.pathFrom(ROOT, OLD))
    }

    @Test
    fun `a damaged file whose groups loop does not hang the tree`() {
        val a = GroupId("a")
        val b = GroupId("b")
        val looped = VaultTree(
            rootId = ROOT,
            recycleBinId = null,
            groups = listOf(
                group(ROOT, null, "Passwords", children = listOf(a)),
                group(a, b, "A", children = listOf(b), entries = 1),
                group(b, a, "B", children = listOf(a), entries = 1),
            ).associateBy { it.id },
        )

        assertEquals(listOf(a), looped.subgroups(ROOT).map { it.id })
        assertTrue(looped.path(b).size <= 3)
        assertEquals(listOf(a, b), looped.descendants(ROOT).map { it.id })
    }
}
