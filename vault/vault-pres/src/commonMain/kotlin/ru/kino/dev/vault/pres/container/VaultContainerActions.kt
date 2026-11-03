package ru.kino.dev.vault.pres.container

import ru.kino.dev.pane.PaneRouter

/**
 * What the container of `vault` is asked to do by its layout - done by [VaultContainerProcessor] (pass-docs,
 * `docs/app/mvi-arc.md`): its tabs, and the routers of its panes - what the screens in them navigate to.
 */
interface VaultContainerActions {

    /** The navigation of the list on the left. */
    val listRouter: PaneRouter

    /** The navigation of the entry [entryId] read on the right: going back closes its tab, "edit" - its form. */
    fun readRouter(entryId: String): PaneRouter

    /** The navigation of the form in the tab [key]: going back - the entry read again; a new one - the tab closed. */
    fun formRouter(key: String): PaneRouter

    /** A tab tapped: it is shown. */
    fun onSelectTab(key: String)

    /** The cross of a tab - its "back": read - the tab closed; a form - it asks, as on a phone. */
    fun onCloseTab(key: String)

    /** "Back" of the system: the list goes one group up. */
    fun onGroupUp()
}
