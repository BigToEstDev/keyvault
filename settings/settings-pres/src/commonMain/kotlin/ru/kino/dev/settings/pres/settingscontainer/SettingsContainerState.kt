package ru.kino.dev.settings.pres.settingscontainer

import ru.kino.dev.settings.pres.settings.SettingsItem

/**
 * What the container of the settings holds on a wide window (pass-docs, Step 101): the page open on the right - one,
 * no tabs; the main settings screen on the left is always the same.
 *
 * @param page the page open; null - none, the right is empty
 */
data class SettingsContainerState(val page: SettingsItem? = null) {

    /** The panes to keep - the list and the page open; the page left is cleared with its processor. */
    val paneKeys: Set<String> = setOfNotNull(SETTINGS_LIST_PANE, page?.let(::settingsPagePane))
}

/** The pages the container opens on the right: the presets are a container of their own, over it (Step 101). */
internal val SettingsItem.isContainerPage: Boolean get() = this != SettingsItem.Presets

/** The key of the pane of the main settings screen. */
const val SETTINGS_LIST_PANE = "settings"

/** The key of the pane of the page [item]. */
fun settingsPagePane(item: SettingsItem): String = "settings-page:${item.name}"
