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

/** The locked database, in place of `vault`: the lock pops `vault` whole, unlocking starts it at the root. */
@Serializable
data class LockedRoute(val dbKey: String)

/** The root group of the open database - the first screen of `vault`. Its groups below are `vault`'s own. */
@Serializable
data class VaultRootRoute(val dbKey: String)

/**
 * The settings, from the main screen of `vault`. Inside the graph of `vault`: the database is open there, and
 * the lock pops them with it (pass-docs, Step 52). [dbKey] - the open database, for its own settings: biometrics
 * first (Step 59).
 */
@Serializable
data class SettingsRoute(val dbKey: String)

/**
 * The recycle bin of the open database [dbKey]: its entries, to restore or remove for good (pass-docs, Step 70). Its
 * screen is in `vault`; the way in is the main screen of `vault` and the recycle bin of `settings`.
 */
@Serializable
data class TrashRoute(val dbKey: String)

/**
 * A preset of one's own: [presetId] - the one to edit, null - a new one. Its screen is in `settings` (pass-docs,
 * Step 62); `vault` opens it from the choice of presets of an entry. No database in it: presets are one list for
 * every database (Step 44).
 *
 * @param readSaved a new one made - its reading in place of the form, as made from the list of the presets (pass-docs,
 *   Step 99, item 8); false - back where it came from: the form of an entry
 */
@Serializable
data class PresetEditRoute(val presetId: String? = null, val readSaved: Boolean = false)
