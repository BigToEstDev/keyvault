package ru.kino.dev.pane

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ru.kino.dev.ui.components.GradientBackground

/**
 * A container of a list and the one page it opens, on the gradient of the whole window - no tabs (pass-docs, Step 101,
 * `docs/dev/navigation.md`). On the left - the card of [list], always there and always as wide, as in
 * [TabbedContainer]. On the right - the card of [page] while one is open ([page] not null); none - the gradient.
 */
@Composable
fun SplitContainer(
    list: @Composable ColumnScope.() -> Unit,
    page: (@Composable ColumnScope.() -> Unit)?,
) {
    GradientBackground {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(PANE_MARGIN),
            horizontalArrangement = Arrangement.spacedBy(PANE_GAP),
        ) {
            PaneCard(modifier = Modifier.width(PANE_LIST_WIDTH).fillMaxHeight(), content = list)
            if (page != null) {
                PaneCard(modifier = Modifier.weight(1f).fillMaxHeight(), content = page)
            }
        }
    }
}
