package ru.kino.dev.settings.pres.settingscontainer

import androidx.compose.runtime.Composable
import ru.kino.dev.pane.PaneHost
import ru.kino.dev.pane.SplitContainer
import ru.kino.dev.settings.pres.appearance.AppearanceScreen
import ru.kino.dev.settings.pres.entries.EntriesScreen
import ru.kino.dev.settings.pres.file.FileScreen
import ru.kino.dev.settings.pres.lock.LockScreen
import ru.kino.dev.settings.pres.privacy.PrivacyScreen
import ru.kino.dev.settings.pres.security.SecurityScreen
import ru.kino.dev.settings.pres.settings.SettingsItem
import ru.kino.dev.settings.pres.settings.SettingsScreen

/**
 * The container of the settings ([SplitContainer]): on the left - the main settings screen; on the right, only while
 * a page is open - that page (pass-docs, Step 101).
 */
@Composable
internal fun SettingsContainerActions.SettingsContainerTablet(
    dbKey: String,
    state: SettingsContainerState,
    panes: PaneHost,
) {
    val page = state.page
    SplitContainer(
        list = {
            panes.Pane(key = SETTINGS_LIST_PANE, router = listRouter) {
                SettingsScreen(dbKey = dbKey)
            }
        },
        page = page?.let {
            {
                panes.Pane(key = settingsPagePane(page), router = pageRouter(page)) {
                    SettingsPage(dbKey = dbKey, item = page)
                }
            }
        },
    )
}

/** The screen of the page [item] - of a phone, as it is. */
@Composable
private fun SettingsPage(dbKey: String, item: SettingsItem) {
    when (item) {
        SettingsItem.Appearance -> AppearanceScreen(dbKey = dbKey)
        SettingsItem.Lock -> LockScreen(dbKey = dbKey)
        SettingsItem.Privacy -> PrivacyScreen()
        SettingsItem.File -> FileScreen(dbKey = dbKey)
        SettingsItem.Entries -> EntriesScreen(dbKey = dbKey)
        SettingsItem.Security -> SecurityScreen(dbKey = dbKey)
        // Their own container, over this one: never a page here
        SettingsItem.Presets -> Unit
    }
}
