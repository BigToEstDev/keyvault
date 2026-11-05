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
    /** The type the app gives the field as the file holds it - see [FieldType]; null - text */
    val type: String? = null,
) {
    // The value is a secret more often than not: it stays out of logs and crash reports
    override fun toString(): String = "EntryFieldData(key=$key, protected=$protected, type=$type)"
}

/** An entry as its screens read it: where it is, what it holds and when it was made and changed. */
data class EntryData(
    /** The group the entry is directly in */
    val groupId: String,
    /** Its strings, in its order */
    val fields: List<EntryFieldData>,
    /** When the entry was created, in seconds since the epoch, UTC; null when the time cannot be read */
    val createdAt: Long? = null,
    /** When the entry was last changed, in seconds since the epoch, UTC; null when it cannot be read */
    val modifiedAt: Long? = null,
    /** Its attachments, in its order - names and sizes, the bytes only on "Download" */
    val attachments: List<EntryAttachmentData> = emptyList(),
)

/**
 * One old version of an entry, as its history lists it: kdbx keeps them inside the entry, and the core
 * puts the previous state there on every update.
 */
data class HistoryVersionData(
    /** Its position in the history, oldest first - not an identity: deleting a version moves the rest */
    val index: Int,
    /** When this version was saved, in seconds since the epoch, UTC; null when the time cannot be read */
    val modifiedAt: Long?,
)

/** An entry as a list shows it: enough for a card, nothing secret. */
data class EntrySummaryData(
    val id: String,
    /** The group the entry is directly in */
    val groupId: String,
    /** Empty when the entry has no title */
    val title: String,
    /** The `UserName` of the entry, empty when it has none */
    val userName: String,
    /** When the entry was created, in seconds since the epoch, UTC */
    val createdAt: Long,
)

/**
 * The entries a term finds, with the term itself: the calls are asynchronous and may finish out of order,
 * so a screen drops the answer to a term the user has already typed past.
 */
data class EntrySearchData(
    val term: String,
    val entries: List<EntrySummaryData>,
)
