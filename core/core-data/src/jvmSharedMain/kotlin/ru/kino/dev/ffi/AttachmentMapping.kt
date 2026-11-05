package ru.kino.dev.ffi

import ru.kino.dev.core.CoreException
import ru.kino.dev.core.EntryAttachmentData
import ru.kino.dev.core.NewAttachmentData

/**
 * The attachments of an entry across the boundary (pass-docs, Step 102): the form lists them (`binary_key_values`),
 * the id the domain knows one by is its hash in the form; new files go with the form - names and sizes in the
 * arguments, the bytes one after another in the binary slot.
 */
internal fun EntryFormDataDto.attachmentsToDomain(): List<EntryAttachmentData> =
    binaryKeyValues.map { EntryAttachmentData(id = it.dataHash, name = it.key, size = it.dataSize) }

/**
 * The same form with only the attachments [keptIds], in that order - those left out are removed. An id the entry
 * has not is refused with kind `DataError`, before the core is asked: the edit names what the entry holds.
 */
internal fun EntryFormDataDto.withAttachmentsKept(keptIds: List<String>): EntryFormDataDto {
    val byId = binaryKeyValues.associateBy { it.dataHash }
    val kept = keptIds.map { id -> byId[id] ?: throw CoreException("DataError", "the entry has no attachment $id") }
    return copy(binaryKeyValues = kept)
}

/** The files added with a form, as the call carries them: the list for the arguments and the bytes for the slot. */
internal class AttachmentSlot(
    val list: List<NewAttachmentDto>,
    /** The bytes of all the files one after another; null - no files, nothing in the slot */
    val bytes: ByteArray?,
) {
    /** The slot is a copy of the files in plaintext: wiped once the call is over, whatever its outcome. */
    fun wipe() {
        bytes?.fill(0)
    }
}

internal fun attachmentSlot(files: List<NewAttachmentData>): AttachmentSlot {
    if (files.isEmpty()) return AttachmentSlot(emptyList(), null)
    val total = files.sumOf { it.bytes.size.toLong() }
    // One array holds them all: past its limit the call is refused here, not cut short
    if (total > Int.MAX_VALUE) throw CoreException("DataError", "the files added are too large for one call")
    val bytes = ByteArray(total.toInt())
    var offset = 0
    files.forEach { file ->
        file.bytes.copyInto(bytes, offset)
        offset += file.bytes.size
    }
    return AttachmentSlot(files.map { NewAttachmentDto(name = it.name, size = it.bytes.size) }, bytes)
}
