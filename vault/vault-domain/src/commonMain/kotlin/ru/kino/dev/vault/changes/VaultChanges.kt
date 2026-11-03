package ru.kino.dev.vault.changes

import kotlinx.coroutines.flow.Flow

/**
 * The changes of the open database, for the screens that show it side by side - one does not see the other change it
 * (pass-docs, Step 98, `docs/dev/navigation.md`). A version, not an event: a screen that was not listening sees the
 * version moved on when it listens again, and tells the change it made itself from one made elsewhere.
 *
 * Every write into the database in memory moves it on - an entry, a group, the history, the recycle bin, a merge, the
 * settings kept in the database. Reading stays as it was: a screen reads again when the version moves.
 */
interface VaultChanges {

    /** The version of the database [dbKey], the current one first; one more with every change. */
    fun versions(dbKey: String): Flow<Long>

    /** The version of the database [dbKey] now. */
    fun current(dbKey: String): Long
}
