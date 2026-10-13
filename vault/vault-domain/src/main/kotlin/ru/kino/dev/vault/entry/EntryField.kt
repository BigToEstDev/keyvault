package ru.kino.dev.vault.entry

import ru.kino.dev.vault.group.EntryId

/**
 * One field of an entry - a string of kdbx: its key in the file, its value, and whether the file keeps it
 * protected.
 *
 * Values are never trimmed: a password of spaces is a password (pass-docs, Step 42, decision 12).
 */
data class EntryField(
    val key: String,
    val value: String,
    val protected: Boolean,
) {
    // The value is a secret more often than not: it stays out of logs and crash reports
    override fun toString(): String = "EntryField(key=$key, protected=$protected)"
}

/** An entry as a card shows it. */
data class EntrySummary(
    val id: EntryId,
    /** Empty when the entry has none: the screen shows its own "untitled" */
    val title: String,
    /** Empty when the entry has none */
    val userName: String,
)
