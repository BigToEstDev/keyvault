package ru.kino.dev.vault.pres.container

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.kino.dev.core.pres.Res
import ru.kino.dev.core.pres.entry_untitled
import ru.kino.dev.pane.PaneHost
import ru.kino.dev.ui.components.ClosableTab
import ru.kino.dev.ui.components.ClosableTabs
import ru.kino.dev.ui.components.GradientBackground
import ru.kino.dev.ui.res.stringResource
import ru.kino.dev.vault.pres.entryread.EntryReadScreen
import ru.kino.dev.vault.pres.grouplist.GroupListScreen

/**
 * The container on a card, the gradient around it: the list of the group on the left, a line, the entries open on
 * the right - their tabs above, the one shown below (pass-docs, Step 98). No entry open - the right part of the card
 * stays empty. The screens are the phone's, each in a pane of its own ([panes]).
 */
@Composable
internal fun VaultContainerActions.VaultContainerTablet(dbKey: String, state: VaultContainerState, panes: PaneHost) {
    GradientBackground {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(CARD_MARGIN),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.width(LIST_WIDTH).fillMaxHeight()) {
                    panes.Pane(key = groupPane(state.group), router = listRouter) {
                        GroupListScreen(dbKey = dbKey, groupId = state.group)
                    }
                }
                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                EntryTabs(dbKey = dbKey, state = state, panes = panes)
            }
        }
    }
}

@Composable
private fun VaultContainerActions.EntryTabs(dbKey: String, state: VaultContainerState, panes: PaneHost) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (state.tabs.isNotEmpty()) {
            val untitled = stringResource(Res.string.entry_untitled)
            ClosableTabs(
                tabs = state.tabs.map { ClosableTab(it.entryId, it.title?.ifEmpty { untitled }.orEmpty()) },
                selected = state.active,
                onSelect = ::onSelectTab,
                onClose = ::onCloseTab,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        state.active?.let { entryId ->
            Box(modifier = Modifier.weight(1f)) {
                panes.Pane(key = entryPane(entryId), router = entryRouter(entryId)) {
                    EntryReadScreen(dbKey = dbKey, entryId = entryId, canGoBack = false)
                }
            }
        }
    }
}

/** The list keeps the width of a phone: wider, its rows stretch to no use. */
private val LIST_WIDTH = 400.dp

private val CARD_MARGIN = 24.dp
