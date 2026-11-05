package ru.kino.dev.vault.attachment

/**
 * The bytes of the attachments of the open database. Their names and sizes come with the entry
 * ([ru.kino.dev.vault.entry.EntryRecord.attachments]); new files go with the form of the entry
 * ([ru.kino.dev.vault.entry.EntryRepository]).
 *
 * Throws [AttachmentFailure] and nothing else.
 */
interface AttachmentRepository {

    /**
     * The bytes of the attachment [id] - of an entry or of one of its versions alike. A copy in plaintext: the
     * caller wipes it once written.
     */
    suspend fun bytes(dbKey: String, id: String): ByteArray
}
