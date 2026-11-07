package ru.kino.dev.vault.sync

/**
 * Every way a merge into the open database can fail, as the screen answers it (pass-docs, Step 108). Unless said
 * otherwise, nothing changed: the core refuses before it touches the open database.
 */
sealed class MergeFailure(message: String, cause: Throwable? = null) : Exception(message, cause) {

    /**
     * The file of this database was saved elsewhere with another password or key file - it does not open with the
     * key in memory. Merged through the settings then, with the credentials it has now.
     */
    class CredentialsChanged(cause: Throwable? = null) :
        MergeFailure("The file is encrypted with other credentials now", cause)

    /** The password or key file given for the other database does not open it. */
    class WrongCredentials(cause: Throwable? = null) : MergeFailure("The credentials do not open the database", cause)

    /** A copy was asked for, the file holds another database - that is an import. */
    class NotSameDatabase(cause: Throwable? = null) : MergeFailure("The file holds another database", cause)

    /** An import was asked for, the file holds a copy of this very database - that is a merge. */
    class SameDatabase(cause: Throwable? = null) : MergeFailure("The file holds this very database", cause)

    /** The file is not a database the core reads - another format, an old one. */
    class UnsupportedFile(cause: Throwable? = null) : MergeFailure("The file is not a supported database", cause)

    /** The file is a database, but damaged - cut short by a sync that did not finish, say. */
    class CorruptedFile(cause: Throwable? = null) : MergeFailure("The file is damaged", cause)

    /** The file could not be read - gone, no access, the provider offline. */
    class FileUnreachable(cause: Throwable? = null) : MergeFailure("The file cannot be read", cause)

    /** The key file picked is over the limit. */
    class KeyFileTooLarge(cause: Throwable? = null) : MergeFailure("The key file is too large", cause)

    /** The key file picked could not be read. */
    class KeyFileUnreadable(cause: Throwable? = null) : MergeFailure("The key file cannot be read", cause)

    /** The database locked meanwhile: its content is out of memory, there is nothing to merge into. */
    class Locked(cause: Throwable? = null) : MergeFailure("The database is locked", cause)

    /** Merged in memory, but writing the file failed: the merge waits there for the next save. */
    class NotSaved(cause: Throwable? = null) : MergeFailure("Merged but not written to the file", cause)

    /** The core refused for another reason - the database closed meanwhile, or a bug. The cause to the log. */
    class Unexpected(cause: Throwable? = null) : MergeFailure("Something went wrong", cause)
}
