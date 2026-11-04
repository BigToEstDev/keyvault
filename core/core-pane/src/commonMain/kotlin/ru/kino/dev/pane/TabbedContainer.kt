package ru.kino.dev.pane

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
import ru.kino.dev.ui.components.ClosableTab
import ru.kino.dev.ui.components.ClosableTabs
import ru.kino.dev.ui.components.GradientBackground

/**
 * A container of a list and the tabs it opens, on the gradient of the whole window: cards, the screens on them with no
 * backdrop of their own (pass-docs, Step 99, `docs/dev/navigation.md`). On the left - the card of [list], always there
 * and always as wide. On the right, only while a tab is open ([selected] not null) - two cards, one over the other:
 * the [tabs] open, and the [page] of the one shown; none open - the gradient.
 */
@Composable
fun TabbedContainer(
    tabs: List<ClosableTab>,
    selected: String?,
    onSelect: (String) -> Unit,
    onClose: (String) -> Unit,
    list: @Composable ColumnScope.() -> Unit,
    page: @Composable ColumnScope.() -> Unit,
) {
    GradientBackground {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(MARGIN),
            horizontalArrangement = Arrangement.spacedBy(GAP),
        ) {
            PaneCard(modifier = Modifier.width(LIST_WIDTH).fillMaxHeight(), content = list)
            if (selected != null) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(TABS_GAP)) {
                    PaneCard(modifier = Modifier.fillMaxWidth()) {
                        ClosableTabs(
                            tabs = tabs,
                            selected = selected,
                            onSelect = onSelect,
                            onClose = onClose,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    PaneCard(modifier = Modifier.fillMaxWidth().weight(1f), content = page)
                }
            }
        }
    }
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

/** Between the tabs open and the one shown: one block of the right. */
private val TABS_GAP = 8.dp
