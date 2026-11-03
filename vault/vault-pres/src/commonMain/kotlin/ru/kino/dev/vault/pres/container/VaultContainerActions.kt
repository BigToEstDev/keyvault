package ru.kino.dev.vault.pres.container

import ru.kino.dev.pane.PaneRouter

/**
 * What the container of `vault` is asked to do by its layout - done by [VaultContainerProcessor] (pass-docs,
 * `docs/app/mvi-arc.md`): its tabs, and the routers of its panes - what the screens in them navigate to.
 */
interface VaultContainerActions {

    /** The navigation of the list on the left. */
    val listRouter: PaneRouter

    /**
     * The navigation of the page [page] of the entry [entryId] - null: its reading. Its form, history, a version - a
     * page over it; "back" - the page under it; back from the reading - the tab closed.
     */
    fun entryRouter(entryId: String, page: EntryPage?): PaneRouter

    /** The navigation of the form of a new entry in the tab [key]: going back - saved or left - closes the tab. */
    fun newRouter(key: String): PaneRouter

    /** A tab tapped: it is shown. */
    fun onSelectTab(key: String)

    /** The cross of a tab - its "back": read - the tab closed; a page over it - back to the one under, a form asks. */
    fun onCloseTab(key: String)

    /** "Back" of the system: the list goes one group up. */
    fun onGroupUp()
}
