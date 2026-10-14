package ru.kino.dev.vault.filter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.kino.dev.vault.entry.EntrySummary
import ru.kino.dev.vault.group.EntryId
import ru.kino.dev.vault.group.GroupId
import ru.kino.dev.vault.group.VaultGroup

class VaultListingTest {

    private val groups = listOf(group("Work"), group("Home"))

    private val entries = listOf(
        entry("old", createdAt = 100),
        entry("new", createdAt = 300),
        entry("middle", createdAt = 200),
    )

    @Test
    fun `the default shows everything, the newest entries first`() {
        val listing = VaultFilter.DEFAULT.applyTo(groups, entries)

        assertEquals(listOf("Work", "Home"), listing.groups.map { it.name })
        assertEquals(listOf("new", "middle", "old"), listing.entries.map { it.title })
    }

    @Test
    fun `the oldest entries can come first`() {
        val listing = VaultFilter(order = CreationOrder.OldestFirst).applyTo(groups, entries)

        assertEquals(listOf("old", "middle", "new"), listing.entries.map { it.title })
    }

    @Test
    fun `groups keep the order of the database whatever the order of the entries`() {
        val newest = VaultFilter(order = CreationOrder.NewestFirst).applyTo(groups, entries)
        val oldest = VaultFilter(order = CreationOrder.OldestFirst).applyTo(groups, entries)

        assertEquals(groups, newest.groups)
        assertEquals(groups, oldest.groups)
    }

    @Test
    fun `entries made in the same second go by title, case aside`() {
        val same = listOf(entry("beta", 5), entry("Alpha", 5), entry("gamma", 5), entry("Delta", 5))

        val listing = VaultFilter.DEFAULT.applyTo(emptyList(), same)

        assertEquals(listOf("Alpha", "beta", "Delta", "gamma"), listing.entries.map { it.title })
    }

    @Test
    fun `only groups or only entries can be shown`() {
        val onlyGroups = VaultFilter(visibility = Visibility.GroupsOnly).applyTo(groups, entries)
        val onlyEntries = VaultFilter(visibility = Visibility.EntriesOnly).applyTo(groups, entries)

        assertEquals(groups, onlyGroups.groups)
        assertEquals(emptyList<EntrySummary>(), onlyGroups.entries)
        assertEquals(emptyList<VaultGroup>(), onlyEntries.groups)
        assertEquals(3, onlyEntries.entries.size)
    }

    @Test
    fun `a filter is default only as it comes`() {
        assertTrue(VaultFilter().isDefault)
        assertFalse(VaultFilter(visibility = Visibility.GroupsOnly).isDefault)
        assertFalse(VaultFilter(order = CreationOrder.OldestFirst).isDefault)
    }

    private fun group(name: String) = VaultGroup(GroupId(name), GroupId("root"), name, emptyList(), emptyList())

    private fun entry(title: String, createdAt: Long) =
        EntrySummary(EntryId(title), GroupId("root"), title, userName = "", createdAt = createdAt)
}
