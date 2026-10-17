package ru.kino.dev.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.kino.dev.core.pres.R

/** One choice of an [ExpandableFab]. */
data class FabItem(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
)

/**
 * A plus that opens a short menu of what can be added, above itself.
 *
 * Open, the plus turns 45 degrees into a cross, and a tap on it closes the menu. Whether it is open is the
 * caller's: the screen closes it after a choice, on "back" and on a tap past the menu.
 */
@Composable
fun ExpandableFab(
    expanded: Boolean,
    onToggle: () -> Unit,
    items: List<FabItem>,
    modifier: Modifier = Modifier,
) {
    val rotation by animateFloatAsState(targetValue = if (expanded) OPEN_ROTATION else 0f, label = "fab")

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(ITEM_SPACING),
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(ITEM_SPACING),
            ) {
                items.forEach { item -> FabMenuItem(item) }
            }
        }
        FloatingActionButton(onClick = onToggle) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(if (expanded) R.string.action_close else R.string.action_add),
                modifier = Modifier.rotate(rotation),
            )
        }
    }
}

/**
 * One choice: the label and the icon on a single card, all of it one target - a label and a small button
 * side by side are two targets, and the gap between them is a miss.
 */
@Composable
private fun FabMenuItem(item: FabItem) {
    Surface(
        onClick = item.onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shadowElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 48.dp)
                .padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = item.label, style = MaterialTheme.typography.labelLarge)
            // The label already says what it does
            Icon(imageVector = item.icon, contentDescription = null)
        }
    }
}

private const val OPEN_ROTATION = 45f

// Between the cards, and between the last card and the plus
private val ITEM_SPACING = 16.dp
