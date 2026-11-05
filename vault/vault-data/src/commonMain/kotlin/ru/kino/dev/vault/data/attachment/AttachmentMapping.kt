package ru.kino.dev.vault.data.attachment

import ru.kino.dev.core.EntryAttachmentData
import ru.kino.dev.core.EntryAttachmentsEdit
import ru.kino.dev.core.NewAttachmentData
import ru.kino.dev.vault.attachment.AttachmentsEdit
import ru.kino.dev.vault.attachment.EntryAttachment
import ru.kino.dev.vault.attachment.NewAttachment

internal fun EntryAttachmentData.toAttachment() = EntryAttachment(id = id, name = name, size = size)

// The same bytes, not a copy: the form wipes them once saved, and a copy here would stay in memory unwiped
internal fun NewAttachment.toData() = NewAttachmentData(name = name, bytes = bytes)

// Kept by name: one name is one attachment, while the same bytes under two names share an id (pass-docs, Step 103)
internal fun AttachmentsEdit.toData() =
    EntryAttachmentsEdit(keptNames = kept.map { it.name }, added = added.map { it.toData() })
