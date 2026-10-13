package ru.kino.dev.core

/**
 * One string of an entry, as the core keeps it - `Title`, `Notes` and every other field alike.
 *
 * The core knows no preset (pass-docs, Step 45): an entry is its strings in the order of the file. Which
 * of them a screen shows and how is decided above this module. A file written by another program may hold
 * two strings under one key; both come here as they are.
 */
data class EntryFieldData(
    val key: String,
    val value: String,
    /** Protected in the file: KeePass protects the password, another program may protect more */
    val protected: Boolean,
) {
    // The value is a secret more often than not: it stays out of logs and crash reports
    override fun toString(): String = "EntryFieldData(key=$key, protected=$protected)"
}

/** An entry as a list shows it: enough for a card, nothing secret. */
data class EntrySummaryData(
    val id: String,
    /** Empty when the entry has no title */
    val title: String,
    /** The `UserName` of the entry, empty when it has none */
    val userName: String,
)
