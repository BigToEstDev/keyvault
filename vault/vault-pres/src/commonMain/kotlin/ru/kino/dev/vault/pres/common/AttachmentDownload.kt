package ru.kino.dev.vault.pres.common

import ru.kino.dev.log.ErrorLog
import ru.kino.dev.vault.attachment.AttachmentFailure
import ru.kino.dev.vault.attachment.AttachmentFiles
import ru.kino.dev.vault.attachment.EntryAttachment

/**
 * "Download" on a screen that reads an entry (pass-docs, Step 103): [attachment] written into the file [uri] picked
 * to save it as. Returns whether it was written; a failure of ours goes to the log - the screen says "could not
 * save the file" either way.
 */
internal suspend fun AttachmentFiles.saveAs(
    dbKey: String,
    attachment: EntryAttachment,
    uri: String,
    log: ErrorLog,
    tag: String,
): Boolean =
    try {
        download(dbKey, attachment.id, uri)
        true
    } catch (failure: AttachmentFailure) {
        // The file, or the attachment gone by now, is no bug of ours
        if (failure is AttachmentFailure.Unexpected) log.unexpected(tag, failure)
        false
    }
