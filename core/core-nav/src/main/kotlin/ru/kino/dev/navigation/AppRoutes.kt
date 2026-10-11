package ru.kino.dev.navigation

import kotlinx.serialization.Serializable

/*
 * Routes that cross the border between feature modules. `welcome` leads into `vault` once the database is
 * open, and `vault` leads into the locked screen of `welcome` - declared in either module, these would make
 * the two depend on each other. Routes inside one module stay in that module.
 *
 * What crosses is the dbKey alone - the uri of the database, not a secret (pass-docs,
 * `docs/android/screens/welcome/decisions.md`, "Граница с vault").
 */

/** The graph of `welcome`: the database is not open yet. Popped whole once it is. */
@Serializable
data object WelcomeGraph

/** The graph of `vault`: the database is open. */
@Serializable
data object VaultGraph

/** The locked database, on top of `vault`: unlocking goes back to where the user was. */
@Serializable
data class LockedRoute(val dbKey: String)

/** The root group of the open database - the first screen of `vault`. Its groups below are `vault`'s own. */
@Serializable
data class VaultRootRoute(val dbKey: String)
