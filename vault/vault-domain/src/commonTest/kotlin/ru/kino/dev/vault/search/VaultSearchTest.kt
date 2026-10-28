package ru.kino.dev.vault.search

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.Test
import ru.kino.dev.core.expectFailure
import ru.kino.dev.core.preset.StandardPreset
import ru.kino.dev.vault.entry.EntryFailure
import ru.kino.dev.vault.entry.EntryField
import ru.kino.dev.vault.entry.FakeEntryRepository
import ru.kino.dev.vault.group.FakeGroupRepository
import ru.kino.dev.vault.group.GroupFailure
import ru.kino.dev.vault.group.EntryCount
import ru.kino.dev.vault.group.GroupId

class VaultSearchTest {

    private val groups = FakeGroupRepository()
    private val entries = FakeEntryRepository()
    private val search = VaultSearch(groups, entries)

    /*
     * Passwords
     * ├── Work            - "Bank of work"
     * │   └── Bank cards  - "Visa" (user "bank-user")
     * ├── Home            - "Bank of home"
     * └── Recycle Bin
     *     └── Old bank    - "Bank of old"
     */
    private val root = groups.rootId
    private val work = groups.add("work", root, "Work")
    private val cards = groups.add("cards", work, "Bank cards")
    private val home = groups.add("home", root, "Home")
    private val old = groups.add("old", root, "Old bank")

    init {
        entries.add("work-bank", work, fields("Bank of work"))
        entries.add("visa", cards, fields("Visa", user = "bank-user"))
        entries.add("home-bank", home, fields("Bank of home"))
        entries.add("old-bank", old, fields("Bank of old"))
        runBlocking { groups.delete(DB, old) }
        groups.calls.clear()
    }

    @Test
    fun `at the root the whole database is searched, groups by name and entries by title or user`() = runTest {
        val result = search.search(DB, root, "bank")

        assertEquals(listOf(cards), result.groups.map { it.id })
        assertEquals(setOf("work-bank", "visa", "home-bank"), result.entries.map { it.id.value }.toSet())
    }

    @Test
    fun `what is found comes with where it lies, from the group searched in`() = runTest {
        val atRoot = search.search(DB, root, "bank")

        assertEquals(listOf("Passwords", "Work"), atRoot.locations[work])
        assertEquals(listOf("Passwords", "Work", "Bank cards"), atRoot.locations[cards])
        assertEquals(listOf("Passwords", "Home"), atRoot.locations[home])

        val inWork = search.search(DB, work, "bank")

        // "Bank of work" lies in Work itself, the group "Bank cards" too; "Visa" - one level down
        assertEquals(listOf("Work"), inWork.locations[work])
        assertEquals(listOf("Work", "Bank cards"), inWork.locations[cards])
        assertEquals(setOf(work, cards), inWork.locations.keys)
    }

    @Test
    fun `a group found comes with the entries under it, for its card`() = runTest {
        groups.addEntries(cards, 2)

        val result = search.search(DB, root, "cards")

        assertEquals(mapOf(cards to EntryCount(direct = 2, nested = 2)), result.entryCounts)
    }

    @Test
    fun `in a group only what lies under it is searched, at any depth`() = runTest {
        val result = search.search(DB, work, "BANK")

        assertEquals(listOf(cards), result.groups.map { it.id })
        assertEquals(setOf("work-bank", "visa"), result.entries.map { it.id.value }.toSet())
    }

    @Test
    fun `the group searched in is not among what it finds`() = runTest {
        val result = search.search(DB, cards, "cards")

        assertEquals(emptyList<GroupId>(), result.groups.map { it.id })
    }

    @Test
    fun `nothing in the recycle bin is found`() = runTest {
        val result = search.search(DB, root, "old")

        assertEquals(emptyList<GroupId>(), result.groups.map { it.id })
        assertEquals(emptyList<String>(), result.entries.map { it.id.value })
    }

    @Test
    fun `a blank term finds nothing and asks nothing`() = runTest {
        val result = search.search(DB, root, "   ")

        assertEquals("   ", result.term)
        assertEquals(emptyList<GroupId>(), result.groups.map { it.id })
        assertEquals(emptyList<String>(), result.entries.map { it.id.value })
        assertEquals(emptyList<String>(), groups.calls)
        assertEquals(emptyList<String>(), entries.calls)
    }

    @Test
    fun `the term comes back as it was typed`() = runTest {
        assertEquals(" Bank ", search.search(DB, root, " Bank ").term)
    }

    @Test
    fun `a failure of the tree or of the entries goes up as it is`() = runTest {
        val treeFailure = GroupFailure.Unexpected(IllegalStateException())
        groups.failures["tree"] = treeFailure
        assertSame(treeFailure, expectFailure<GroupFailure> { search.search(DB, root, "bank") })

        groups.failures.clear()
        val entryFailure = EntryFailure.Unexpected(IllegalStateException())
        entries.failures["search"] = entryFailure
        assertSame(entryFailure, expectFailure<EntryFailure> { search.search(DB, root, "bank") })
    }

    private fun fields(title: String, user: String = "") = listOf(
        EntryField(StandardPreset.TITLE, title, protected = false),
        EntryField(StandardPreset.USER_NAME, user, protected = false),
    )

    private companion object {
        const val DB = "content://db"
    }
}
