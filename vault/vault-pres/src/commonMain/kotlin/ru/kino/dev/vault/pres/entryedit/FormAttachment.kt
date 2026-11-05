package ru.kino.dev.vault.pres.entryedit

import ru.kino.dev.vault.attachment.AttachmentsEdit
import ru.kino.dev.vault.attachment.EntryAttachment
import ru.kino.dev.vault.attachment.NewAttachment

/**
 * An attachment on the form of an entry (pass-docs, Step 103): one of the entry, kept, or a file added here - its
 * bytes held until the form is saved or left.
 */
sealed interface FormAttachment {
    val name: String
    val size: Long

    /** One of the entry, as read; removed, it stays in the version the save puts into the history. */
    data class Kept(val attachment: EntryAttachment) : FormAttachment {
        override val name: String get() = attachment.name
        override val size: Long get() = attachment.size
    }

    /** A file added on the form - after those kept, as the core saves them. */
    data class Added(val file: NewAttachment) : FormAttachment {
        override val name: String get() = file.name
        override val size: Long get() = file.size
    }
}

/** The attachments of the form as the save takes them: those kept, then those added. */
internal fun List<FormAttachment>.toEdit(): AttachmentsEdit = AttachmentsEdit(
    kept = filterIsInstance<FormAttachment.Kept>().map { it.attachment },
    added = added(),
)

/** The files added on the form - to save, or to wipe when it is left. */
internal fun List<FormAttachment>.added(): List<NewAttachment> = filterIsInstance<FormAttachment.Added>().map { it.file }
