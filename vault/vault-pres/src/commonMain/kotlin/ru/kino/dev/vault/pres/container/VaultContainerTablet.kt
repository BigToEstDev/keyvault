package ru.kino.dev.vault.pres.container

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import ru.kino.dev.core.pres.entry_untitled
import ru.kino.dev.pane.PaneHost
import ru.kino.dev.ui.components.ClosableTab
import ru.kino.dev.ui.components.ClosableTabs
import ru.kino.dev.ui.components.GradientBackground
import ru.kino.dev.ui.res.stringResource
import ru.kino.dev.vault.pres.entryread.EntryReadScreen
import ru.kino.dev.vault.pres.grouplist.GroupListScreen

/**
 * The container on the gradient of the whole window: two cards, the screens on them with no backdrop of their own
 * (pass-docs, Step 99). On the left - the card of `vault`, the list of the group, always there and always as wide. On
 * the right - the card of the entries, their tabs above, the one shown below, there only while an entry is open; none
 * open - the gradient. The screens are the phone's, each in a pane of its own ([panes]).
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
            PaneCard(modifier = Modifier.width(LIST_WIDTH)) {
                panes.Pane(key = groupPane(state.group), router = listRouter) {
                    GroupListScreen(dbKey = dbKey, groupId = state.group)
                }
            }
            state.active?.let { entryId ->
                PaneCard(modifier = Modifier.weight(1f)) {
                    EntryTabs(dbKey = dbKey, entryId = entryId, state = state, panes = panes)
                }
            }
        }
    }
}

@Composable
private fun VaultContainerActions.EntryTabs(dbKey: String, entryId: String, state: VaultContainerState, panes: PaneHost) {
    val untitled = stringResource(Res.string.entry_untitled)
    ClosableTabs(
        tabs = state.tabs.map { ClosableTab(it.entryId, it.title?.ifEmpty { untitled }.orEmpty()) },
        selected = entryId,
        onSelect = ::onSelectTab,
        onClose = ::onCloseTab,
        modifier = Modifier.fillMaxWidth(),
    )
    Box(modifier = Modifier.fillMaxSize()) {
        panes.Pane(key = entryPane(entryId), router = entryRouter(entryId)) {
            EntryReadScreen(dbKey = dbKey, entryId = entryId, canGoBack = false)
        }
    }
}

/** A card of the container: the tone and the corners of the card of `welcome` (pass-docs, Step 94). */
@Composable
private fun PaneCard(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(modifier = Modifier.fillMaxSize(), content = content)
    }
}

/** The list keeps the width of a phone: wider, its rows stretch to no use. */
private val LIST_WIDTH = 400.dp

/** From the edges of the window. */
private val MARGIN = 24.dp

/** Between the cards. */
private val GAP = 16.dp
