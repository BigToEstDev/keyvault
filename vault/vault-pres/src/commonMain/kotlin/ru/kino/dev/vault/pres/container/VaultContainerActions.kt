package ru.kino.dev.vault.pres.container

import ru.kino.dev.pane.PaneRouter

/**
 * What the container of `vault` is asked to do by its layout - done by [VaultContainerProcessor] (pass-docs,
 * `docs/app/mvi-arc.md`): its tabs, and the routers of its panes - what the screens in them navigate to.
 */
interface VaultContainerActions {

    /** The navigation of the list on the left. */
    val listRouter: PaneRouter

    /** The navigation of the entry [entryId] on the right: going back closes its tab. */
    fun entryRouter(entryId: String): PaneRouter

    /** A tab tapped: its entry is shown. */
    fun onSelectTab(entryId: String)

    /** The cross of a tab: the entry is closed, the tab next to it shown. */
    fun onCloseTab(entryId: String)

    /** "Back" of the system: the list goes one group up. */
    fun onGroupUp()
}
