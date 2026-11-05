package ru.kino.dev.vault.pres.trashentry

/** What an entry of the recycle bin asks of its screen once. */
sealed interface TrashEntrySideEffect {

    /** "Download": the file to save the attachment as, the [name] suggested - its own (pass-docs, Step 103). */
    data class SaveAttachment(val name: String) : TrashEntrySideEffect
}
