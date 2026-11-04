package ru.kino.dev.vault.pres.trashcontainer

import ru.kino.dev.pane.PaneRouter

/**
 * What the container of the recycle bin is asked to do by its layout - done by [TrashContainerProcessor] (pass-docs,
 * `docs/app/mvi-arc.md`): its tabs, and the routers of its panes.
 */
interface TrashContainerActions {

    /** The navigation of the list of the bin: an entry - its tab; "back" - the app's, back to `vault`. */
    val listRouter: PaneRouter

    /** The navigation of the entry [entryId]: "back" - restored, removed or gone from the bin - its tab closed. */
    fun entryRouter(entryId: String): PaneRouter

    /** A tab tapped: it is shown. */
    fun onSelectTab(entryId: String)

    /** The cross of a tab: it closes. */
    fun onCloseTab(entryId: String)
}
