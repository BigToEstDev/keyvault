package ru.kino.dev.vault.group

/** The id of a group - the core's uuid. A type of its own, so a group id cannot go where an entry id goes. */
@JvmInline
value class GroupId(val value: String)

/** The id of an entry - the core's uuid. */
@JvmInline
value class EntryId(val value: String)
