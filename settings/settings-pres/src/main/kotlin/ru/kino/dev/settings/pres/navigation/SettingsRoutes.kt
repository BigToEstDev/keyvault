package ru.kino.dev.settings.pres.navigation

import kotlinx.serialization.Serializable
import ru.kino.dev.settings.pres.settings.SettingsItem

/** The appearance of the app: the theme for now (pass-docs, Step 55). */
@Serializable
internal data object AppearanceRoute

/** Where a card of the settings leads; null while the screen behind it is not made yet. */
internal val SettingsItem.route: Any?
    get() = when (this) {
        SettingsItem.Appearance -> AppearanceRoute
        else -> null
    }
