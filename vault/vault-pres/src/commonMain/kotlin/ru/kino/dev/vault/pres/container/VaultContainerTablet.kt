package ru.kino.dev.vault.pres.container

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.kino.dev.core.pres.Res
import ru.kino.dev.core.pres.entry_title_new
import ru.kino.dev.core.pres.entry_untitled
import ru.kino.dev.pane.PaneHost
import ru.kino.dev.ui.components.ClosableTab
import ru.kino.dev.ui.components.ClosableTabs
import ru.kino.dev.ui.components.GradientBackground
import ru.kino.dev.ui.res.stringResource
import ru.kino.dev.vault.pres.entryedit.EntryEditScreen
import ru.kino.dev.vault.pres.entryhistory.EntryHistoryScreen
import ru.kino.dev.vault.pres.entryread.EntryReadScreen
import ru.kino.dev.vault.pres.entryversion.EntryVersionScreen
import ru.kino.dev.vault.pres.grouplist.GroupListScreen

/**
 * The container on the gradient of the whole window: cards, the screens on them with no backdrop of their own
 * (pass-docs, Step 99). On the left - the card of `vault`, the list of the group, always there and always as wide. On
 * the right, only while an entry is open - two cards, one over the other: the entries open, as tabs, and the one shown
 * (item 5) - read, or a page over it: its form, history, a version (Step 98, items 6, 7); none open - the gradient. The screens are the phone's, each in a pane
 * of its own ([panes]).
 */
@Composable
internal fun VaultContainerActions.VaultContainerTablet(dbKey: String, state: VaultContainerState, panes: PaneHost) {
    GradientBackground {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(MARGIN),
            horizontalArrangement = Arrangement.spacedBy(GAP),
        ) {
            PaneCard(modifier = Modifier.width(LIST_WIDTH).fillMaxHeight()) {
                panes.Pane(key = groupPane(state.group), router = listRouter) {
                    GroupListScreen(dbKey = dbKey, groupId = state.group)
                }
            }
            state.shown?.let { tab ->
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(TABS_GAP)) {
                    PaneCard(modifier = Modifier.fillMaxWidth()) {
                        OpenEntries(shown = tab, state = state)
                    }
                    PaneCard(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        TabPage(dbKey = dbKey, tab = tab, panes = panes)
                    }
                }
            }
        }
    }
}

/** The page of [tab] - in its place, one at a time (pass-docs, Step 98, items 6, 7); no "back" of its own: the cross is. */
@Composable
private fun VaultContainerActions.TabPage(dbKey: String, tab: EntryTab, panes: PaneHost) {
    when (tab) {
        is EntryTab.Entry -> panes.Pane(key = tab.pane, router = entryRouter(tab.entryId, tab.top)) {
            when (val page = tab.top) {
                null -> EntryReadScreen(dbKey = dbKey, entryId = tab.entryId, canGoBack = false)
                EntryPage.Edit -> EntryEditScreen(dbKey = dbKey, groupId = null, entryId = tab.entryId, canGoBack = false)
                EntryPage.History -> EntryHistoryScreen(dbKey = dbKey, entryId = tab.entryId, canGoBack = false)
                is EntryPage.Version ->
                    EntryVersionScreen(dbKey = dbKey, entryId = tab.entryId, index = page.index, canGoBack = false)
            }
        }

        is EntryTab.New -> panes.Pane(key = tab.pane, router = newRouter(tab.key)) {
            EntryEditScreen(dbKey = dbKey, groupId = tab.groupId, entryId = null, canGoBack = false)
        }
    }
}

/** The tabs open, [shown] shown. */
@Composable
private fun VaultContainerActions.OpenEntries(shown: EntryTab, state: VaultContainerState) {
    val untitled = stringResource(Res.string.entry_untitled)
    val new = stringResource(Res.string.entry_title_new)
    ClosableTabs(
        tabs = state.tabs.map { tab ->
            val title = when (tab) {
                is EntryTab.Entry -> tab.title?.ifEmpty { untitled }.orEmpty()
                is EntryTab.New -> new
            }
            ClosableTab(tab.key, title)
        },
        selected = shown.key,
        onSelect = ::onSelectTab,
        onClose = ::onCloseTab,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** A card of the container: the tone and the corners of the card of `welcome` (pass-docs, Step 94). */
@Composable
private fun PaneCard(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(content = content)
    }
}

/** The list keeps the width of a phone: wider, its rows stretch to no use. */
private val LIST_WIDTH = 400.dp

/** From the edges of the window. */
private val MARGIN = 24.dp

/** Between the cards. */
private val GAP = 16.dp

/** Between the entries open and the one shown: one block of the right. */
private val TABS_GAP = 8.dp
