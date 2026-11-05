package ru.kino.dev.vault.attachment

/**
 * Every way adding a file to the form or downloading an attachment can fail, as a screen of `vault` answers it.
 * [AttachmentRepository] and [AttachmentFiles] throw nothing else.
 *
 * The message is for the log; the screen shows its own text for each case.
 */
sealed class AttachmentFailure(message: String, cause: Throwable? = null) : Exception(message, cause) {

    /** The file picked is larger than [ATTACHMENT_MAX_BYTES] - it is not added. */
    class TooLarge : AttachmentFailure("The file is larger than an attachment may be")

    /** The file picked could not be read, or the one to download into could not be written. */
    class FileFailed(cause: Throwable? = null) : AttachmentFailure("The file could not be read or written", cause)

    /** The attachment is gone from the database - the entry changed on another screen, or a merge removed it. */
    class NotFound(cause: Throwable? = null) : AttachmentFailure("The attachment is not in the database", cause)

    /** Ours: a bug, or a state that should not happen. "Something went wrong", and the cause to the log. */
    class Unexpected(cause: Throwable? = null) : AttachmentFailure("Something went wrong", cause)
}
