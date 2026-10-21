package ru.kino.dev.settings.pres.navigation

import kotlinx.serialization.Serializable
import ru.kino.dev.settings.pres.settings.SettingsItem

/** The appearance of the app: the theme for now (pass-docs, Step 55). */
@Serializable
internal data object AppearanceRoute

/** How the database locks and how one gets in - [dbKey] for the biometrics of the database (pass-docs, Step 59). */
@Serializable
internal data class LockRoute(val dbKey: String)

/** What the app lets out of its screen: screenshots for now (pass-docs, Step 61). */
@Serializable
internal data object PrivacyRoute

/** The file of the database [dbKey]: its name, the file, the format, the compression (pass-docs, Step 63). */
@Serializable
internal data class FileRoute(val dbKey: String)

/** The presets: whether they are used, the list of those of one's own (pass-docs, Step 62). */
@Serializable
internal data object PresetsRoute

/** Where a card of the settings of [dbKey] leads; null while the screen behind it is not made yet. */
internal fun SettingsItem.route(dbKey: String): Any? = when (this) {
    SettingsItem.Appearance -> AppearanceRoute
    SettingsItem.Lock -> LockRoute(dbKey)
    SettingsItem.Privacy -> PrivacyRoute
    SettingsItem.Presets -> PresetsRoute
    SettingsItem.File -> FileRoute(dbKey)
    else -> null
}
