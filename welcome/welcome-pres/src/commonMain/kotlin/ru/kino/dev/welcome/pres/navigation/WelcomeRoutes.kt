package ru.kino.dev.welcome.pres.navigation

import kotlinx.serialization.Serializable

/*
 * Routes inside `welcome`. The ones other modules lead to - the graph itself and the locked screen - are in
 * core-nav, `AppRoutes.kt`.
 */

/** The first screen: open a database or create one. */
@Serializable
internal data object WelcomeRoute

/**
 * Entering the password of a database that is not open.
 *
 * @param pickedUri a file just picked - on the welcome screen or by tapping the name of the database; null
 * for the remembered database. Not remembered until it opens: a file picked by mistake replaces nothing.
 */
@Serializable
internal data class UnlockRoute(val pickedUri: String? = null)

/**
 * Creating a database, from the master password to the `create` call. Its own graph so that the draft of
 * the creation lives exactly as long as the graph does (pass-docs, Step 27, `CreateDraft`).
 */
@Serializable
internal data object CreateGraph

@Serializable
internal data object MasterPasswordRoute

@Serializable
internal data object KeyFileBackupRoute

/**
 * After the database was created and is open - offering biometrics. Inside the creation graph, so the
 * draft with the password lives until it is encrypted; the screens of typing are gone by then.
 */
@Serializable
internal data class BiometricSetupRoute(val dbKey: String)
