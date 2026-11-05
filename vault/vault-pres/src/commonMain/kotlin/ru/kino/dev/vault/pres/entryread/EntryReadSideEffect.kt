package ru.kino.dev.vault.pres.entryread

/** What the reading of an entry asks of its screen once. */
sealed interface EntryReadSideEffect {

    /** "Download": the file to save the attachment as, the [name] suggested - its own (pass-docs, Step 103). */
    data class SaveAttachment(val name: String) : EntryReadSideEffect
}
