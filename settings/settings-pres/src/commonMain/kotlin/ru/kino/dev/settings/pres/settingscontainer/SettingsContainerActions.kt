package ru.kino.dev.settings.pres.settingscontainer

import ru.kino.dev.pane.PaneRouter
import ru.kino.dev.settings.pres.settings.SettingsItem

/**
 * What the container of the settings is asked to do by its layout - done by [SettingsContainerProcessor] (pass-docs,
 * `docs/app/mvi-arc.md`): its one tab, and the routers of its panes.
 */
interface SettingsContainerActions {

    /** The navigation of the main settings screen: a page - open on the right; the presets, "back" - the app's. */
    val listRouter: PaneRouter

    /** The navigation of the page [item]: "back" - the page closed, the right empty; the rest - the app's. */
    fun pageRouter(item: SettingsItem): PaneRouter

    /** The tab tapped: the only one, shown already - nothing to do. */
    fun onSelectTab(key: String)

    /** The cross of the tab: the page closed, the right empty. */
    fun onCloseTab(key: String)
}
