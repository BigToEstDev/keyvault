package ru.kino.dev.vault.pres.container

import androidx.compose.runtime.Composable
import ru.kino.dev.core.pres.Res
import ru.kino.dev.core.pres.entry_title_new
import ru.kino.dev.core.pres.entry_untitled
import ru.kino.dev.pane.PaneHost
import ru.kino.dev.pane.TabbedContainer
import ru.kino.dev.ui.components.ClosableTab
import ru.kino.dev.ui.res.stringResource
import ru.kino.dev.vault.pres.entryedit.EntryEditScreen
import ru.kino.dev.vault.pres.entryhistory.EntryHistoryScreen
import ru.kino.dev.vault.pres.entryread.EntryReadScreen
import ru.kino.dev.vault.pres.entryversion.EntryVersionScreen
import ru.kino.dev.vault.pres.grouplist.GroupListScreen

/**
 * The container of `vault` ([TabbedContainer], pass-docs, Step 99): on the left - the list of the group; on the
 * right, only while an entry is open - the entries open, as tabs, and the one shown (item 5) - read, or a page over
 * it: its form, history, a version (Step 98, items 6, 7). The screens are the phone's, each in a pane of its own
 * ([panes]).
 */
@Composable
internal fun VaultContainerActions.VaultContainerTablet(dbKey: String, state: VaultContainerState, panes: PaneHost) {
    val untitled = stringResource(Res.string.entry_untitled)
    val new = stringResource(Res.string.entry_title_new)
    TabbedContainer(
        tabs = state.tabs.map { tab ->
            val title = when (tab) {
                is EntryTab.Entry -> tab.title?.ifEmpty { untitled }.orEmpty()
                is EntryTab.New -> new
            }
            ClosableTab(tab.key, title)
        },
        selected = state.shown?.key,
        tabLimit = if (state.tabLimit) VAULT_MAX_TABS else null,
        onTabLimitDismiss = ::onTabLimitDismiss,
        onSelect = ::onSelectTab,
        onClose = ::onCloseTab,
        list = {
            panes.Pane(key = groupPane(state.group), router = listRouter) {
                GroupListScreen(dbKey = dbKey, groupId = state.group)
            }
        },
        page = { state.shown?.let { TabPage(dbKey = dbKey, tab = it, panes = panes) } },
    )
}

/**
 * The page of [tab] - in its place, one at a time (pass-docs, Step 98, items 6, 7): a page over the reading goes back
 * to the one under it by its arrow, as on a phone; the reading has none - the cross of the tab closes it (item 8).
 */
@Composable
private fun VaultContainerActions.TabPage(dbKey: String, tab: EntryTab, panes: PaneHost) {
    when (tab) {
        is EntryTab.Entry -> panes.Pane(key = tab.pane, router = entryRouter(tab.entryId, tab.top)) {
            when (val page = tab.top) {
                null -> EntryReadScreen(dbKey = dbKey, entryId = tab.entryId, canGoBack = false)
                EntryPage.Edit -> EntryEditScreen(dbKey = dbKey, groupId = null, entryId = tab.entryId)
                EntryPage.History -> EntryHistoryScreen(dbKey = dbKey, entryId = tab.entryId)
                is EntryPage.Version -> EntryVersionScreen(dbKey = dbKey, entryId = tab.entryId, index = page.index)
            }
        }

        is EntryTab.New -> panes.Pane(key = tab.pane, router = newRouter(tab.key)) {
            EntryEditScreen(dbKey = dbKey, groupId = tab.groupId, entryId = null)
        }
    }
}
