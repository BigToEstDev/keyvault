package ru.kino.dev.vault.sync

/**
 * Every way copying the file of the open database can fail (pass-docs, Step 111). The open database is never changed
 * by a copy: at most it is saved first.
 */
sealed class DatabaseCopyFailure(message: String, cause: Throwable? = null) : Exception(message, cause) {

    /** The file picked to save the copy into is the file of the open database itself. */
    class OwnFile : DatabaseCopyFailure("The file is the one of the open database")

    /** A change waited in memory and its save failed: the file is behind, it is not copied. */
    class NotSaved(cause: Throwable? = null) : DatabaseCopyFailure("The database is not written to its file", cause)

    /** The file of the database was written elsewhere: what is in it is not this database - synced first. */
    class ChangedElsewhere : DatabaseCopyFailure("The file of the database was changed elsewhere")

    /** The file of the database could not be read. */
    class Unreadable(cause: Throwable? = null) : DatabaseCopyFailure("The file of the database cannot be read", cause)

    /** The copy could not be written - no access, gone, no room, the provider failed. */
    class NotWritten(cause: Throwable? = null) : DatabaseCopyFailure("The copy is not written", cause)
}
