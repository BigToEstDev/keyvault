package ru.kino.dev.vault.group

/**
 * Every way creating, renaming, deleting or reading groups can fail, as a screen of `vault` answers it.
 *
 * The group repository and [GroupEditor] throw nothing else: the string kinds of the core stay below the
 * data layer. A processor catches this type in one place and branches with an exhaustive `when`.
 *
 * An exception rather than a result type, the way `WelcomeFailure` is: `kotlin.Result` in `runCatching`
 * would swallow cancellation along with the failures.
 *
 * The message is for the log; the screen shows its own text for each case.
 */
sealed class GroupFailure(message: String, cause: Throwable? = null) : Exception(message, cause) {

    /** Nothing but spaces. Shown under the name field. */
    class NameEmpty : GroupFailure("The name of a group cannot be empty")

    /** Longer than [GroupNameValidator.MAX_LENGTH] characters. Shown under the name field, with the limit. */
    class NameTooLong : GroupFailure("The name of a group is longer than ${GroupNameValidator.MAX_LENGTH}") {
        val maxLength: Int get() = GroupNameValidator.MAX_LENGTH
    }

    /** Another group in the same parent is already called that. Shown under the name field. */
    class NameTaken : GroupFailure("A group with this name is already there")

    /**
     * The group is gone - a merge removed it, or it was deleted on another screen. The screen reloads the
     * tree rather than show a group that is not there.
     */
    class GroupNotFound(cause: Throwable? = null) : GroupFailure("The group is not in the database", cause)

    /**
     * The change is made in the database, but writing the file failed. The change is not lost: it waits in
     * memory for the next save. How a screen answers - retry, merge with what is in the file - is
     * pass-docs, Step 35, point 4.
     */
    class NotSaved(cause: Throwable? = null) : GroupFailure("The change is made but not written to the file", cause)

    /** Ours: a bug, or a state that should not happen. "Something went wrong", and the cause to the log. */
    class Unexpected(cause: Throwable? = null) : GroupFailure("Something went wrong", cause)
}
