package ru.kino.dev.vault.pres.entryedit

/** What the form of an entry asks of its screen once. */
sealed interface EntryEditSideEffect {

    /** "+ Add attachment": the file to attach (pass-docs, Step 103). */
    data object PickAttachment : EntryEditSideEffect
}
