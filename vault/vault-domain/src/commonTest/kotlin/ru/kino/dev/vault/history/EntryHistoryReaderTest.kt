package ru.kino.dev.vault.history

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import ru.kino.dev.core.expectFailure
import ru.kino.dev.core.preset.StandardPreset.TITLE
import ru.kino.dev.vault.entry.EntryFailure
import ru.kino.dev.vault.entry.EntryField
import ru.kino.dev.vault.entry.FakeEntryRepository
import ru.kino.dev.vault.group.EntryId
import ru.kino.dev.vault.group.GroupId

class EntryHistoryReaderTest {

    private val entries = FakeEntryRepository()
    private val mail =
        entries.add("mail", GroupId("work"), listOf(EntryField(TITLE, "Mail", protected = false)), createdAt = 1_000L)

    @Test
    fun `the history comes with the name of the entry, the versions newest first and when it was made`() = runTest {
        entries.putHistory(mail, 1_000L, 2_000L, 3_000L)

        val history = EntryHistoryReader(entries, FakeEntryHistoryRepository(entries)).read(DB, mail)

        assertEquals("Mail", history.title)
        assertEquals(listOf(2, 1, 0), history.versions.map { it.index })
        assertEquals(listOf(3_000L, 2_000L, 1_000L), history.versions.map { it.modified })
        assertEquals(1_000L, history.created)
        assertEquals(GroupId("work"), history.groupId)
    }

    @Test
    fun `an entry never changed has no versions, only when it was made`() = runTest {
        val history = EntryHistoryReader(entries, FakeEntryHistoryRepository(entries)).read(DB, mail)

        assertEquals(emptyList<HistoryVersion>(), history.versions)
        assertEquals(1_000L, history.created)
    }

    @Test
    fun `a version whose time cannot be read is left out, the others keep their index`() = runTest {
        val versions = object : EntryHistoryRepository by FakeEntryHistoryRepository(entries) {
            override suspend fun versions(dbKey: String, entryId: EntryId) =
                listOf(HistoryVersion(0, 1_000L), HistoryVersion(1, null), HistoryVersion(2, 3_000L))
        }

        val history = EntryHistoryReader(entries, versions).read(DB, mail)

        assertEquals(listOf(2, 0), history.versions.map { it.index })
    }

    @Test
    fun `the history of an entry that is gone is refused`() = runTest {
        expectFailure<EntryFailure.EntryNotFound> {
            EntryHistoryReader(entries, FakeEntryHistoryRepository(entries)).read(DB, EntryId("gone"))
        }
    }

    private companion object {
        const val DB = "content://db"
    }
}
