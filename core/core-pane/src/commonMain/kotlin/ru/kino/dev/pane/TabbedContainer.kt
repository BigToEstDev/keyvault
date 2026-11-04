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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import ru.kino.dev.core.pres.Res
import ru.kino.dev.core.pres.action_ok
import ru.kino.dev.core.pres.tabs_limit_text
import ru.kino.dev.core.pres.tabs_limit_title
import ru.kino.dev.ui.components.ClosableTab
import ru.kino.dev.ui.components.ClosableTabs
import ru.kino.dev.ui.components.GradientBackground
import ru.kino.dev.ui.res.pluralStringResource
import ru.kino.dev.ui.res.stringResource

/**
 * A container of a list and the tabs it opens, on the gradient of the whole window: cards, the screens on them with no
 * backdrop of their own (pass-docs, Step 99, `docs/dev/navigation.md`). On the left - the card of [list], always there
 * and always as wide. On the right, only while a tab is open ([selected] not null) - two cards, one over the other:
 * the [tabs] open, and the [page] of the one shown; none open - the gradient.
 *
 * @param tabLimit the most tabs open, reached as one more was asked for - "close one first" over the container; null -
 *   none (pass-docs, Step 99, item 10)
 */
@Composable
fun TabbedContainer(
    tabs: List<ClosableTab>,
    selected: String?,
    onSelect: (String) -> Unit,
    onClose: (String) -> Unit,
    list: @Composable ColumnScope.() -> Unit,
    page: @Composable ColumnScope.() -> Unit,
    tabLimit: Int? = null,
    onTabLimitDismiss: () -> Unit = {},
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

    // One button: nothing to choose - a tab is closed by its cross
    if (tabLimit != null) {
        AlertDialog(
            onDismissRequest = onTabLimitDismiss,
            title = { Text(stringResource(Res.string.tabs_limit_title)) },
            text = { Text(pluralStringResource(Res.plurals.tabs_limit_text, tabLimit, tabLimit)) },
            confirmButton = { TextButton(onClick = onTabLimitDismiss) { Text(stringResource(Res.string.action_ok)) } },
        )
    }
}

/** Between the tabs open and the one shown: one block of the right. */
private val TABS_GAP = 8.dp
