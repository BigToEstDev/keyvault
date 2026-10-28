package ru.kino.dev.vault.entries

import kotlinx.coroutines.test.runTest
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.Test
import ru.kino.dev.core.FieldProtection
import ru.kino.dev.core.HistoryLimits
import ru.kino.dev.core.expectFailure
import ru.kino.dev.vault.FakeVaultDatabase
import java.io.IOException

class EntrySettingsEditorTest {

    private val settings = FakeEntrySettingsRepository()
    private val database = FakeVaultDatabase()
    private val editor = EntrySettingsEditor(settings, database)

    @Test
    fun `the protection chosen keeps the history and saves`() = runTest {
        val protection = FieldProtection.DEFAULT.copy(title = true)

        editor.setProtection(DB, protection)

        assertEquals(listOf(settings.entries), settings.changes)
        assertEquals(protection, settings.entries.protection)
        assertEquals(HistoryLimits(10, SIX_MB), settings.entries.history)
        assertEquals(listOf("save"), database.calls)
    }

    @Test
    fun `the same protection is not a change - nothing is written`() = runTest {
        editor.setProtection(DB, FieldProtection.DEFAULT)

        assertEquals(emptyList<Any>(), settings.changes)
        assertEquals(emptyList<String>(), database.calls)
    }

    @Test
    fun `the limits chosen keep the protection and save`() = runTest {
        editor.setHistory(DB, HistoryLimits(maxItems = null, maxSize = SIX_MB))

        assertEquals(HistoryLimits(null, SIX_MB), settings.entries.history)
        assertEquals(FieldProtection.DEFAULT, settings.entries.protection)
        assertEquals(listOf("save"), database.calls)
    }

    @Test
    fun `the same limits are not a change - nothing is written`() = runTest {
        editor.setHistory(DB, HistoryLimits(10, SIX_MB))

        assertEquals(emptyList<String>(), database.calls)
    }

    @Test
    fun `a file not written is not saved, and the change stays in memory`() = runTest {
        database.saveFailure = IOException("provider gone")

        expectFailure<EntrySettingsFailure.NotSaved> { editor.setHistory(DB, HistoryLimits(5, SIX_MB)) }

        assertEquals(HistoryLimits(5, SIX_MB), settings.entries.history)
    }

    @Test
    fun `a lower limit, or one where there was none, trims`() {
        val current = HistoryLimits(10, SIX_MB)

        assertTrue(HistoryLimits(5, SIX_MB).trims(current))
        assertTrue(HistoryLimits(10, SIX_MB / 2).trims(current))
        assertTrue(HistoryLimits(50, SIX_MB).trims(HistoryLimits(null, SIX_MB)))
    }

    @Test
    fun `a higher limit, or none, keeps every version`() {
        val current = HistoryLimits(10, SIX_MB)

        assertFalse(HistoryLimits(20, SIX_MB).trims(current))
        assertFalse(HistoryLimits(null, null).trims(current))
        assertFalse(current.trims(current))
    }

    private companion object {
        const val DB = "content://docs/passwords.kdbx"
        const val SIX_MB = 6L * 1024 * 1024
    }
}
