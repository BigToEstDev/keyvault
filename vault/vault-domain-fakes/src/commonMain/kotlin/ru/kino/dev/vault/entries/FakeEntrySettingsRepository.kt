package ru.kino.dev.vault.entries

import ru.kino.dev.core.DatabaseEntries
import ru.kino.dev.core.FieldProtection
import ru.kino.dev.core.HistoryLimits

/**
 * The settings of the entries in memory: what [read] answers, replaced by [change]. [failure], when set, is thrown
 * by every call instead.
 */
class FakeEntrySettingsRepository(
    var entries: DatabaseEntries = DatabaseEntries(
        protection = FieldProtection.DEFAULT,
        history = HistoryLimits(maxItems = 10, maxSize = 6L * 1024 * 1024),
    ),
) : EntrySettingsRepository {

    var failure: EntrySettingsFailure? = null

    /** Every change asked for, in order. */
    val changes = mutableListOf<DatabaseEntries>()

    override suspend fun read(dbKey: String): DatabaseEntries {
        failure?.let { throw it }
        return entries
    }

    override suspend fun change(dbKey: String, entries: DatabaseEntries) {
        failure?.let { throw it }
        changes += entries
        this.entries = entries
    }
}
