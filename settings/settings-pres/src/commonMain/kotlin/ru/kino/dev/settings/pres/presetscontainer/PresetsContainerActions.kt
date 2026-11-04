package ru.kino.dev.settings.pres.presetscontainer

import ru.kino.dev.pane.PaneRouter

/**
 * What the container of the presets is asked to do by its layout - done by [PresetsContainerProcessor] (pass-docs,
 * `docs/app/mvi-arc.md`): its tabs, and the routers of its panes.
 */
interface PresetsContainerActions {

    /** The navigation of the list: a preset, a new one - its tab; "back" - the app's, back to the settings. */
    val listRouter: PaneRouter

    /**
     * The navigation of the form in the tab [key]: "back", saved - the tab closed; the tab the container was opened
     * with, from the form of an entry - the app's, back to that form.
     */
    fun tabRouter(key: String): PaneRouter

    /** A tab tapped: it is shown. */
    fun onSelectTab(key: String)

    /** The cross of a tab: its form closes - with changes it asks first. */
    fun onCloseTab(key: String)
}
