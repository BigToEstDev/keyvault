package ru.kino.dev.vault.entries

import ru.kino.dev.core.DatabaseEntries

/**
 * The settings of the entries of the open database - the private fields and the history limits (pass-docs,
 * Step 65). Of `vault`: the database is open, and its file is written by `vault`. Changes go through
 * [EntrySettingsEditor], which writes the file.
 *
 * Throws [EntrySettingsFailure] and nothing else.
 */
interface EntrySettingsRepository {

    /** The private fields and the history limits, as the file has them. */
    suspend fun read(dbKey: String): DatabaseEntries

    /** Sets them - in memory only, the file is not written. A lower limit trims the history of every entry. */
    suspend fun change(dbKey: String, entries: DatabaseEntries)
}

/** Every way reading or changing the settings of the entries can fail, as the settings answer it. */
sealed class EntrySettingsFailure(message: String, cause: Throwable? = null) : Exception(message, cause) {

    /** The change is made in the database, but writing the file failed: it waits in memory for the next save. */
    class NotSaved(cause: Throwable? = null) : EntrySettingsFailure("The change is made but not written to the file", cause)

    /** The core refused - the database closed meanwhile, or a bug. The cause to the log. */
    class Unexpected(cause: Throwable? = null) : EntrySettingsFailure("Something went wrong", cause)
}
