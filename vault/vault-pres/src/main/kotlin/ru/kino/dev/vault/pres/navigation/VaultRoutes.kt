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

/** Reading an entry - a stub until the step of the entries. */
@Serializable
internal data class EntryReadRoute(val dbKey: String, val entryId: String)

/** Creating an entry in [parentGroupId], or editing [entryId] - a stub until the step of the entries. */
@Serializable
internal data class EntryEditRoute(val dbKey: String, val parentGroupId: String, val entryId: String? = null)
