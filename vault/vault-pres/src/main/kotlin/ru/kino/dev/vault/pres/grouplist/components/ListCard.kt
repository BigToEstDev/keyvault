package ru.kino.dev.vault.pres.grouplist.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import ru.kino.dev.vault.pres.common.EditDeleteMenu

/**
 * A card of the list - a group or an entry: an icon and a title. A tap opens it; a long press opens a menu -
 * edit, delete - with its top corner where the finger is.
 *
 * Whether the menu is open is the card's own: it is a moment of the gesture, not of the screen.
 *
 * @param subtitle a second, smaller line under the title - where a card found by a search lies; cut from
 * its start when long, so the nearest group stays in sight
 */
@Composable
internal fun ListCard(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String?,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menu by remember { mutableStateOf(false) }
    var pressedAt by remember { mutableStateOf(Offset.Zero) }
    var cardHeight by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    Box(modifier = modifier.onSizeChanged { cardHeight = it.height }) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    // Only watches where the finger went down - consumes nothing, so the click below works
                    .pointerInput(Unit) {
                        awaitEachGesture { pressedAt = awaitFirstDown(requireUnconsumed = false).position }
                    }
                    .combinedClickable(onClick = onOpen, onLongClick = { menu = true })
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                icon()
                Column(modifier = Modifier.padding(start = 24.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.StartEllipsis,
                        )
                    }
                }
            }
        }
        // The menu is placed below its anchor, the card, and moved by the offset: up by the card's height and
        // down to the finger, so its top corner is where the press was. Near an edge it is kept on screen
        EditDeleteMenu(
            expanded = menu,
            onDismiss = { menu = false },
            onDelete = onDelete,
            onEdit = onEdit,
            offset = with(density) { DpOffset(pressedAt.x.toDp(), (pressedAt.y - cardHeight).toDp()) },
        )
    }
}
