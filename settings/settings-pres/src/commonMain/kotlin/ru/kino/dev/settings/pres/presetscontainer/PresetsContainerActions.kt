package ru.kino.dev.settings.pres.presetscontainer

import ru.kino.dev.pane.PaneRouter

/**
 * What the container of the presets is asked to do by its layout - done by [PresetsContainerProcessor] (pass-docs,
 * `docs/app/mvi-arc.md`): its tabs, and the routers of its panes.
 */
interface PresetsContainerActions {

    /** The navigation of the list: a preset - its tab, read; its form, a new one - in a tab; "back" - the app's. */
    val listRouter: PaneRouter

    /**
     * The navigation of the preset [presetId] - its reading, or its form when [editing]: the form - over the reading;
     * "back" - the page under it, from the reading - the tab closed; past the reading - deleted, closed - the tab closed.
     */
    fun presetRouter(presetId: String?, editing: Boolean): PaneRouter

    /**
     * The navigation of the new preset in the tab [key]: saved from the list - the tab reads it; "back" - the tab
     * closed; the tab the container was opened with, from the form of an entry - the app's, back to that form.
     */
    fun newRouter(key: String): PaneRouter

    /** A tab tapped: it is shown. */
    fun onSelectTab(key: String)

    /** The cross of a tab: it closes - a form with changes asks first. */
    fun onCloseTab(key: String)
}
