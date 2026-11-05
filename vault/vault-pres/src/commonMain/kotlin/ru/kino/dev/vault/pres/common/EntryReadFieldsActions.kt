package ru.kino.dev.vault.pres.common

/**
 * What the fields of an entry to read ask for ([EntryReadFields]) - the same on every screen that shows them: the
 * reading of an entry, an old version of it, an entry in the recycle bin (pass-docs, `docs/app/mvi-arc.md`).
 */
interface EntryReadFieldsActions : AttachmentDownloadActions {

    fun onTogglePassword()

    /** Shows or hides the value of the protected own field at [index]. */
    fun onToggleOwn(index: Int)

    /** Shows or hides the value of the field of the preset under [key], private by the database. */
    fun onToggleKey(key: String)
}

/**
 * "Download" by an attachment ([EntryReadFields]): a screen that reads the entry has it, an old version has none -
 * as it has no "copy" (pass-docs, Step 103); empty by default.
 */
interface AttachmentDownloadActions {

    /** "Download" by the attachment at [index]: asks where to save it. */
    fun onDownload(index: Int) {}
}
