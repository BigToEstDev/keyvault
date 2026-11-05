package ru.kino.dev.vault.attachment

import ru.kino.dev.core.DatabaseFileException
import ru.kino.dev.core.DatabaseFiles
import kotlin.coroutines.cancellation.CancellationException

/**
 * Attachments and the files of the device (pass-docs, Step 103): a file picked to add to the form, an attachment
 * downloaded into a file picked to save it as.
 *
 * Throws [AttachmentFailure] and nothing else.
 */
class AttachmentFiles(
    private val files: DatabaseFiles,
    private val attachments: AttachmentRepository,
) {

    /**
     * The file [uri] as a new attachment, named as its provider tells and numbered past the names [taken] by the
     * form ([AttachmentNames.unique]). A file larger than [ATTACHMENT_MAX_BYTES] is [AttachmentFailure.TooLarge]:
     * refused by the size the provider tells, before a byte is read, and by the bytes for a provider that tells
     * none - never read past one byte over the limit, so a video picked by mistake is not held whole.
     */
    suspend fun read(uri: String, taken: Collection<String>): NewAttachment = fileCall {
        val size = files.size(uri)
        if (size != null && size > ATTACHMENT_MAX_BYTES) throw AttachmentFailure.TooLarge()
        val bytes = files.readAtMost(uri, ATTACHMENT_MAX_BYTES + 1)
        if (bytes.size > ATTACHMENT_MAX_BYTES) {
            bytes.fill(0)
            throw AttachmentFailure.TooLarge()
        }
        NewAttachment(AttachmentNames.unique(files.displayName(uri).orEmpty(), taken), bytes)
    }

    /**
     * Writes the attachment [id] into the file [uri] picked to save it as. Its bytes are wiped once written, written
     * or not.
     */
    suspend fun download(dbKey: String, id: String, uri: String) {
        val bytes = attachments.bytes(dbKey, id)
        try {
            fileCall { files.writeReplacing(uri, bytes) }
        } finally {
            bytes.fill(0)
        }
    }

    private suspend fun <T> fileCall(block: suspend () -> T): T =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: AttachmentFailure) {
            throw e
        } catch (e: DatabaseFileException) {
            throw AttachmentFailure.FileFailed(e)
        } catch (e: Exception) {
            throw AttachmentFailure.Unexpected(e)
        }
}
