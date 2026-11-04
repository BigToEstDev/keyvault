package ru.kino.dev.vault.pres.trashcontainer

import androidx.compose.runtime.Composable
import ru.kino.dev.core.pres.Res
import ru.kino.dev.core.pres.entry_untitled
import ru.kino.dev.pane.PaneHost
import ru.kino.dev.pane.TabbedContainer
import ru.kino.dev.ui.components.ClosableTab
import ru.kino.dev.ui.res.stringResource
import ru.kino.dev.vault.pres.trash.TrashScreen
import ru.kino.dev.vault.pres.trashentry.TrashEntryScreen

/**
 * The container of the bin ([TabbedContainer]): on the left - the list of the bin; on the right, only while an entry
 * is open - the entries open, as tabs, and the one shown, read. Its cross closes it, not "back".
 */
@Composable
internal fun TrashContainerActions.TrashContainerTablet(dbKey: String, state: TrashContainerState, panes: PaneHost) {
    val untitled = stringResource(Res.string.entry_untitled)
    TabbedContainer(
        tabs = state.tabs.map { ClosableTab(it.entryId, it.title?.ifEmpty { untitled }.orEmpty()) },
        selected = state.shown?.entryId,
        tabLimit = if (state.tabLimit) TRASH_MAX_TABS else null,
        onTabLimitDismiss = ::onTabLimitDismiss,
        onSelect = ::onSelectTab,
        onClose = ::onCloseTab,
        list = {
            panes.Pane(key = TRASH_LIST_PANE, router = listRouter) {
                TrashScreen(dbKey = dbKey)
            }
        },
        page = {
            state.shown?.let { tab ->
                panes.Pane(key = trashEntryPane(tab.entryId), router = entryRouter(tab.entryId)) {
                    TrashEntryScreen(dbKey = dbKey, entryId = tab.entryId, canGoBack = false)
                }
            }
        },
    )
}
