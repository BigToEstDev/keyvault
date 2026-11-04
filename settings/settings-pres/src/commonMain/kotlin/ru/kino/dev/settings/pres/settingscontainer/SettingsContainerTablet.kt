package ru.kino.dev.settings.pres.settingscontainer

import androidx.compose.runtime.Composable
import ru.kino.dev.pane.PaneHost
import ru.kino.dev.pane.TabbedContainer
import ru.kino.dev.settings.pres.appearance.AppearanceScreen
import ru.kino.dev.settings.pres.entries.EntriesScreen
import ru.kino.dev.settings.pres.file.FileScreen
import ru.kino.dev.settings.pres.lock.LockScreen
import ru.kino.dev.settings.pres.privacy.PrivacyScreen
import ru.kino.dev.settings.pres.security.SecurityScreen
import ru.kino.dev.settings.pres.settings.SettingsItem
import ru.kino.dev.settings.pres.settings.SettingsScreen
import ru.kino.dev.ui.components.ClosableTab
import ru.kino.dev.ui.res.stringResource

/**
 * The container of the settings ([TabbedContainer]): on the left - the main settings screen; on the right, only while
 * a page is open - one tab, titled by the page, and the page; another chosen - the tab opens it instead, no second one
 * (pass-docs, Step 101). Its cross closes it, not "back".
 */
@Composable
internal fun SettingsContainerActions.SettingsContainerTablet(
    dbKey: String,
    state: SettingsContainerState,
    panes: PaneHost,
) {
    val page = state.page
    TabbedContainer(
        tabs = listOfNotNull(page?.let { ClosableTab(settingsPagePane(it), stringResource(it.title)) }),
        selected = page?.let(::settingsPagePane),
        onSelect = ::onSelectTab,
        onClose = ::onCloseTab,
        list = {
            panes.Pane(key = SETTINGS_LIST_PANE, router = listRouter) {
                SettingsScreen(dbKey = dbKey)
            }
        },
        page = {
            if (page != null) {
                panes.Pane(key = settingsPagePane(page), router = pageRouter(page)) {
                    SettingsPage(dbKey = dbKey, item = page)
                }
            }
        },
    )
}

/** The screen of the page [item] - of a phone, as it is, but with no "back": the cross of the tab closes it. */
@Composable
private fun SettingsPage(dbKey: String, item: SettingsItem) {
    when (item) {
        SettingsItem.Appearance -> AppearanceScreen(dbKey = dbKey, canGoBack = false)
        SettingsItem.Lock -> LockScreen(dbKey = dbKey, canGoBack = false)
        SettingsItem.Privacy -> PrivacyScreen(canGoBack = false)
        SettingsItem.File -> FileScreen(dbKey = dbKey, canGoBack = false)
        SettingsItem.Entries -> EntriesScreen(dbKey = dbKey, canGoBack = false)
        SettingsItem.Security -> SecurityScreen(dbKey = dbKey, canGoBack = false)
        // Their own container, over this one: never a page here
        SettingsItem.Presets -> Unit
    }
}
