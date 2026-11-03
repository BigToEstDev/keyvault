package ru.kino.dev.vault.pres.container

/**
 * What the container of `vault` is asked to do by its own layout - done by [VaultContainerProcessor] (pass-docs,
 * `docs/app/mvi-arc.md`). What its panes ask goes through the routers of the processor.
 */
interface VaultContainerActions {

    /** A tab tapped: its entry is shown. */
    fun onSelectTab(entryId: String)

    /** The cross of a tab: the entry is closed, the tab next to it shown. */
    fun onCloseTab(entryId: String)

    /** "Back" of the system: the list goes one group up. */
    fun onGroupUp()
}
