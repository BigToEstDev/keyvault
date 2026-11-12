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

/** The security of the database [dbKey]: the credentials, the cipher, the key derivation (pass-docs, Step 67). */
@Serializable
internal data class SecurityRoute(val dbKey: String)

/** Another database file brought into [dbKey]: a copy merged, a different database imported (pass-docs, Step 108). */
@Serializable
internal data class MergeRoute(val dbKey: String)

/** About the app (pass-docs, Step 122) - of the app, not of the database open. */
@Serializable
internal data object AboutRoute

/** The presets: whether they are used, the list of those of one's own (pass-docs, Step 62). */
@Serializable
internal data object PresetsRoute

/**
 * A preset read (pass-docs, Step 99, item 8): [presetId] - one of one's own, null - the standard one. Opened from the
 * list of the presets; from it - its form.
 */
@Serializable
internal data class PresetReadRoute(val presetId: String? = null)

/** Where a card of the settings of [dbKey] leads - every card has its screen since the recycle bin (Step 70). */
internal fun SettingsItem.route(dbKey: String): Any = when (this) {
    SettingsItem.Appearance -> AppearanceRoute(dbKey)
    SettingsItem.Lock -> LockRoute(dbKey)
    SettingsItem.Privacy -> PrivacyRoute
    SettingsItem.Presets -> PresetsRoute
    SettingsItem.File -> FileRoute(dbKey)
    SettingsItem.Entries -> EntriesRoute(dbKey)
    SettingsItem.Security -> SecurityRoute(dbKey)
    SettingsItem.Sync -> MergeRoute(dbKey)
    SettingsItem.About -> AboutRoute
}
