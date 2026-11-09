package ru.kino.dev.vault.data.entries

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import ru.kino.dev.core.DatabaseEntries
import ru.kino.dev.core.FakeNativeCore
import ru.kino.dev.core.FieldProtection
import ru.kino.dev.core.HistoryLimits
import ru.kino.dev.core.OpenedDatabase
import ru.kino.dev.core.expectFailure
import ru.kino.dev.vault.data.changes.VaultChangesImpl
import ru.kino.dev.vault.entries.EntrySettingsFailure

class EntrySettingsRepositoryImplTest {

    private val core = FakeNativeCore().apply {
        opened[DB] = OpenedDatabase(DB, "Passwords", null)
    }
    private val changes = VaultChangesImpl()
    private val repository = EntrySettingsRepositoryImpl(core, changes)

    @Test
    fun `the settings are the core's`() = runTest {
        assertEquals(core.databaseEntries, repository.read(DB))
    }

    @Test
    fun `a change goes to the core and nowhere else`() = runTest {
        val changed = DatabaseEntries(FieldProtection.DEFAULT.copy(notes = true), HistoryLimits(null, null))

        repository.change(DB, changed)

        assertEquals(changed, core.databaseEntries)
        assertEquals(listOf("changeDatabaseEntries $DB"), core.journal)
    }

    @Test
    fun `a database closed meanwhile is unexpected`() = runTest {
        core.opened.remove(DB)

        expectFailure<EntrySettingsFailure.Unexpected> { repository.read(DB) }
        expectFailure<EntrySettingsFailure.Unexpected> { repository.change(DB, core.databaseEntries) }
    }

    private companion object {
        const val DB = "content://docs/passwords.kdbx"
    }
}
