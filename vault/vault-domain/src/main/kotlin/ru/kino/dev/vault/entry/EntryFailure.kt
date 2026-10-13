package ru.kino.dev.vault.entry

/**
 * Every way reading, creating, changing or deleting entries can fail, as a screen of `vault` answers it.
 *
 * The entry repository and [EntryEditor] throw nothing else: the string kinds of the core stay below the
 * data layer. An exception rather than a result type, as [ru.kino.dev.vault.group.GroupFailure] is.
 *
 * The message is for the log; the screen shows its own text for each case.
 */
sealed class EntryFailure(message: String, cause: Throwable? = null) : Exception(message, cause) {

    /**
     * Fields the entry cannot be saved with - every one of them at once, so the screen marks them all
     * (pass-docs, Step 42, decision 16).
     */
    class InvalidFields(val problems: List<FieldProblem>) : EntryFailure("The entry has fields it cannot be saved with")

    /** The entry is gone - a merge removed it, or it was deleted on another screen. */
    class EntryNotFound(cause: Throwable? = null) : EntryFailure("The entry is not in the database", cause)

    /** The group to create the entry in is gone. */
    class GroupNotFound(cause: Throwable? = null) : EntryFailure("The group is not in the database", cause)

    /**
     * The change is made in the database, but writing the file failed. The change is not lost: it waits in
     * memory for the next save.
     */
    class NotSaved(cause: Throwable? = null) : EntryFailure("The change is made but not written to the file", cause)

    /** Ours: a bug, or a state that should not happen. "Something went wrong", and the cause to the log. */
    class Unexpected(cause: Throwable? = null) : EntryFailure("Something went wrong", cause)
}

/** What is wrong with one field of an entry. */
sealed class FieldProblem {

    /** A required field of the preset is empty. Shown under the field. */
    data class Required(val key: String) : FieldProblem()

    /** The name of the own field at [index] in [EntryContent.ownFields] cannot be used. Shown under it. */
    data class BadName(val index: Int, val problem: NameProblem) : FieldProblem()
}

/** What is wrong with the name of an own field. */
enum class NameProblem {
    /** Nothing but spaces */
    EMPTY,

    /** Another field of the entry - of the preset or an own one - is already called that */
    TAKEN,
}
