package ru.kino.dev.settings.pres.navigation

import kotlinx.serialization.Serializable
import ru.kino.dev.settings.pres.settings.SettingsItem

/**
 * The appearance of the app (pass-docs, Step 55) - of the app, not of [dbKey]; the database open says whether its
 * user names may be in the list (pass-docs, Step 56, item 5).
 */
@Serializable
internal data class AppearanceRoute(val dbKey: String)

/** How the database locks and how one gets in - [dbKey] for the biometrics of the database (pass-docs, Step 59). */
@Serializable
internal data class LockRoute(val dbKey: String)

/** What the app lets out of its screen: screenshots for now (pass-docs, Step 61). */
@Serializable
internal data object PrivacyRoute

/** The file of the database [dbKey]: its name, the file, the format, the compression (pass-docs, Step 63). */
@Serializable
internal data class FileRoute(val dbKey: String)

/** The entries of the database [dbKey]: the hidden fields, the limits of the history (pass-docs, Step 65). */
@Serializable
internal data class EntriesRoute(val dbKey: String)

/** The presets: whether they are used, the list of those of one's own (pass-docs, Step 62). */
@Serializable
internal data object PresetsRoute

/** Where a card of the settings of [dbKey] leads; null while the screen behind it is not made yet. */
internal fun SettingsItem.route(dbKey: String): Any? = when (this) {
    SettingsItem.Appearance -> AppearanceRoute(dbKey)
    SettingsItem.Lock -> LockRoute(dbKey)
    SettingsItem.Privacy -> PrivacyRoute
    SettingsItem.Presets -> PresetsRoute
    SettingsItem.File -> FileRoute(dbKey)
    SettingsItem.Entries -> EntriesRoute(dbKey)
    else -> null
}
