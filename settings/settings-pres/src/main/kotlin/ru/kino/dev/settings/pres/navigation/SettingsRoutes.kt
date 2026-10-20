package ru.kino.dev.settings.pres.navigation

import kotlinx.serialization.Serializable
import ru.kino.dev.settings.pres.settings.SettingsItem

/** The appearance of the app: the theme for now (pass-docs, Step 55). */
@Serializable
internal data object AppearanceRoute

/** How the database locks and how one gets in - [dbKey] for the biometrics of the database (pass-docs, Step 59). */
@Serializable
internal data class LockRoute(val dbKey: String)

/** Where a card of the settings of [dbKey] leads; null while the screen behind it is not made yet. */
internal fun SettingsItem.route(dbKey: String): Any? = when (this) {
    SettingsItem.Appearance -> AppearanceRoute
    SettingsItem.Lock -> LockRoute(dbKey)
    else -> null
}
