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
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
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
 * caller's: the screen closes it after a choice, and on "back".
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
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items.forEach { item -> FabMenuItem(item) }
            }
        }
        FloatingActionButton(onClick = onToggle) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(if (expanded) R.string.fab_close else R.string.fab_open),
                modifier = Modifier.rotate(rotation),
            )
        }
    }
}

@Composable
private fun FabMenuItem(item: FabItem) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            onClick = item.onClick,
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ) {
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
        // The label already says what it does
        SmallFloatingActionButton(onClick = item.onClick) {
            Icon(imageVector = item.icon, contentDescription = null)
        }
    }
}

private const val OPEN_ROTATION = 45f
