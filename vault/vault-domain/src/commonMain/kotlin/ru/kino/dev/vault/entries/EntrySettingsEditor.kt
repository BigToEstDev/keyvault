package ru.kino.dev.vault.entries

import ru.kino.dev.core.FieldProtection
import ru.kino.dev.core.HistoryLimits
import ru.kino.dev.vault.VaultDatabaseRepository
import ru.kino.dev.vault.lock.DatabaseGuard
import kotlin.coroutines.cancellation.CancellationException

/**
 * Changing the settings of the entries from the settings: one choice, one write of the file (pass-docs, Step 65).
 * The same value again is not a change: nothing is written.
 *
 * Each change runs inside [guard], so a lock waits for its write to end.
 *
 * Throws [EntrySettingsFailure] and nothing else.
 */
class EntrySettingsEditor(
    private val settings: EntrySettingsRepository,
    private val database: VaultDatabaseRepository,
    private val guard: DatabaseGuard = DatabaseGuard(),
) {

    /** Makes the fields of [protection] private on every entry and saves. */
    suspend fun setProtection(dbKey: String, protection: FieldProtection): Unit = guard.exclusive {
        val current = settings.read(dbKey)
        if (protection == current.protection) return@exclusive
        settings.change(dbKey, current.copy(protection = protection))
        save(dbKey)
    }

    /**
     * Keeps the history to [history] and saves. A lower limit trims the history of every entry at once and for
     * good - the screen asks first ([HistoryLimits.trims]).
     */
    suspend fun setHistory(dbKey: String, history: HistoryLimits): Unit = guard.exclusive {
        val current = settings.read(dbKey)
        if (history == current.history) return@exclusive
        settings.change(dbKey, current.copy(history = history))
        save(dbKey)
    }

    private suspend fun save(dbKey: String) {
        try {
            database.save(dbKey)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw EntrySettingsFailure.NotSaved(e)
        }
    }
}

/**
 * Whether going from [current] to these limits may drop versions already kept: either limit lower, or set where
 * there was none.
 */
fun HistoryLimits.trims(current: HistoryLimits): Boolean =
    lower(maxItems?.toLong(), current.maxItems?.toLong()) || lower(maxSize, current.maxSize)

private fun lower(limit: Long?, current: Long?): Boolean = limit != null && (current == null || limit < current)
