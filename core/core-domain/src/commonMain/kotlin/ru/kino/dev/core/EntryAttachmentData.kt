package ru.kino.dev.core

/**
 * One attachment of an entry, as the file holds it: the list of the entry in KDBX (`Binary`), seen by other programs
 * too (pass-docs, Step 102). Only its name and size travel with the entry; the bytes come on "Download"
 * ([NativeCore.attachmentBytes]).
 */
data class EntryAttachmentData(
    /** Its bytes in the open database - the same bytes under two names are one id; changes with no edit of it */
    val id: String,
    /** The file name - one name, one attachment in the entry */
    val name: String,
    /** The length of its bytes */
    val size: Long,
)

/**
 * A file added to an entry: kept by the app until "Save" of the form, then handed to the core with it - bytes
 * linked with no entry would be gone after the next write of the file. Its [bytes] are the caller's to wipe after
 * the save.
 */
class NewAttachmentData(
    val name: String,
    val bytes: ByteArray,
) {
    // The content of a file is not for logs and crash reports
    override fun toString(): String = "NewAttachmentData(name=$name, size=${bytes.size})"
}

/**
 * The attachments of an entry after an edit: those kept, by [EntryAttachmentData.name], in their order, and the
 * files [added] after them - the order the core saves in. What is left out of [keptNames] is removed; the history
 * of the entry keeps it in the version it was in.
 *
 * Kept by name, not by id: one name is one attachment of the entry, while the same bytes under two names share one
 * id (pass-docs, Step 103).
 */
data class EntryAttachmentsEdit(
    val keptNames: List<String>,
    val added: List<NewAttachmentData>,
)
