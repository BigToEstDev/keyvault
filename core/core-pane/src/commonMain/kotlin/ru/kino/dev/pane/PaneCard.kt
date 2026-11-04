package ru.kino.dev.pane

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A card of a container - [TabbedContainer], [SplitContainer]: the tone and the corners of the card of `welcome`
 * (pass-docs, Step 94).
 */
@Composable
internal fun PaneCard(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(content = content)
    }
}

/** The list of a container keeps the width of a phone: wider, its rows stretch to no use. */
internal val PANE_LIST_WIDTH = 400.dp

/** A container from the edges of the window. */
internal val PANE_MARGIN = 24.dp

/** Between the cards of a container. */
internal val PANE_GAP = 16.dp
