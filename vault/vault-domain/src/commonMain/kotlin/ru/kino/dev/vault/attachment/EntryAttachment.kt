package ru.kino.dev.vault.attachment

/**
 * The largest file a person adds to an entry: 2 MB, 2 x 1024 x 1024 bytes (pass-docs, Step 103). Only adding is held
 * to it - an attachment of another program that is larger is read and downloaded as it is.
 */
const val ATTACHMENT_MAX_BYTES: Int = 2 * 1024 * 1024

/**
 * The most attachments a person brings an entry to by adding (pass-docs, Step 103, item 5) - as many as own fields in
 * their card. Only adding is held to it: an entry of another program with more is read and saved as it is, and gets a
 * file added once it has fewer.
 */
const val ATTACHMENTS_MAX_PER_ENTRY: Int = 10

/**
 * One attachment of an entry or of its version, as the file holds it - its name and size; the bytes come on
 * "Download" ([AttachmentFiles.download]).
 */
data class EntryAttachment(
    /** Its bytes in the open database - what "Download" asks for; the same bytes under two names share it */
    val id: String,
    /** The file name - one name, one attachment in the entry */
    val name: String,
    /** The length of its bytes */
    val size: Long,
)

/**
 * A file added on the form of an entry, held until "Save". Its [bytes] are a file in plaintext: whoever holds the
 * form wipes them ([wipe]) once it is saved or left.
 */
class NewAttachment(
    val name: String,
    val bytes: ByteArray,
) {
    val size: Long get() = bytes.size.toLong()

    /** Overwrites the bytes with zeros - after the save, or when the form is left without one. */
    fun wipe() {
        bytes.fill(0)
    }

    // The content of a file is not for logs and crash reports
    override fun toString(): String = "NewAttachment(name=$name, size=${bytes.size})"
}

/**
 * The attachments of an entry after its form: those [kept] of the entry, in their order, and the files [added] after
 * them - the order the core saves in. An attachment of the entry not in [kept] is removed; its version keeps it.
 */
data class AttachmentsEdit(
    val kept: List<EntryAttachment>,
    val added: List<NewAttachment>,
) {
    /** The names the entry has after the edit, in order. */
    val names: List<String> get() = kept.map { it.name } + added.map { it.name }

    /** Files are added past [ATTACHMENTS_MAX_PER_ENTRY] - the form does not let that happen. */
    val addsPastLimit: Boolean get() = added.isNotEmpty() && names.size > ATTACHMENTS_MAX_PER_ENTRY

    /** Whether the edit leaves the entry with exactly [attachments] - then it is no change. */
    fun keepsExactly(attachments: List<EntryAttachment>): Boolean = added.isEmpty() && kept == attachments
}
