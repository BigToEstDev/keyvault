package ru.kino.dev.settings.pres.navigation

import kotlinx.serialization.Serializable
import ru.kino.dev.settings.pres.settings.SettingsItem

/** The appearance of the app: the theme for now (pass-docs, Step 55). */
@Serializable
internal data object AppearanceRoute

/** How the database locks and how one gets in (pass-docs, Step 59). */
@Serializable
internal data object LockRoute

/** Where a card of the settings leads; null while the screen behind it is not made yet. */
internal val SettingsItem.route: Any?
    get() = when (this) {
        SettingsItem.Appearance -> AppearanceRoute
        SettingsItem.Lock -> LockRoute
        else -> null
    }
