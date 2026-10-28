package ru.kino.dev.vault.history

import ru.kino.dev.vault.entry.EntryOpener
import ru.kino.dev.vault.entry.OpenedEntry
import ru.kino.dev.vault.group.EntryId

/**
 * An old version of an entry as its screen shows it (pass-docs, Step 51, item 6): read from the history and
 * laid over the preset it linked to then, as [EntryOpener] lays an entry. [OpenedEntry.times] has when the
 * version was saved.
 *
 * Throws [ru.kino.dev.vault.entry.EntryFailure] and nothing else.
 */
class EntryVersionOpener(
    private val history: EntryHistoryRepository,
    private val opener: EntryOpener,
) {

    suspend fun open(dbKey: String, entryId: EntryId, index: Int): OpenedEntry =
        opener.layOut(history.version(dbKey, entryId, index))
}
