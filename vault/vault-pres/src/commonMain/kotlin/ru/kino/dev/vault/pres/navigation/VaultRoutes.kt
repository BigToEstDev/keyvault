package ru.kino.dev.vault.pres.navigation

import kotlinx.serialization.Serializable

/*
 * Routes inside `vault`. The way in from `welcome` - the root of the database - is `VaultRootRoute` in
 * core-nav, because it crosses the border; these do not.
 */

/**
 * A group below the root. Every group opened is its own place on the back stack, so "back" goes up one
 * level, and the path in the top bar is the back stack itself.
 */
@Serializable
internal data class GroupRoute(val dbKey: String, val groupId: String)

/** Reading an entry. */
@Serializable
internal data class EntryReadRoute(val dbKey: String, val entryId: String)

/** The history of an entry: its old versions (pass-docs, Step 51). */
@Serializable
internal data class EntryHistoryRoute(val dbKey: String, val entryId: String)

/**
 * One old version of an entry - at [index] in its history, oldest first. A position, not an identity: the
 * screen reads it once, when it opens (pass-docs, Step 51, item 6).
 */
@Serializable
internal data class EntryVersionRoute(val dbKey: String, val entryId: String, val index: Int)

/*
 * Creating and editing are one screen with two ways in: each route carries exactly what its mode needs, so
 * there is no "neither a group nor an entry" to guard against.
 */

/** Creating an entry inside [groupId]. */
@Serializable
internal data class EntryCreateRoute(val dbKey: String, val groupId: String)

/** Editing the entry [entryId]. */
@Serializable
internal data class EntryEditRoute(val dbKey: String, val entryId: String)

/** An entry of the recycle bin, read - to copy from, restore or delete for good (pass-docs, Step 71). */
@Serializable
internal data class TrashEntryRoute(val dbKey: String, val entryId: String)
