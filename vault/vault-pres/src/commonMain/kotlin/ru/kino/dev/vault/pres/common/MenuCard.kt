package ru.kino.dev.vault.pres.common

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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import ru.kino.dev.ui.components.ItemMenu
import ru.kino.dev.ui.components.MenuItem
import ru.kino.dev.ui.components.contextClickable

/**
 * A card with an icon and a title - a group or an entry of a list, a version of a history. A tap opens it,
 * when there is [onOpen]; the gesture of the context - a long press, the right button on the desktop - opens the menu
 * of [items] with its top corner where it was, when there is one.
 *
 * Whether the menu is open is the card's own: it is a moment of the gesture, not of the screen.
 *
 * @param subtitle a second, smaller line under the title - where a card found by a search lies; cut from
 * its start when long, so the nearest group stays in sight
 */
@Composable
internal fun MenuCard(
    icon: @Composable () -> Unit,
    title: String,
    items: List<MenuItem>,
    modifier: Modifier = Modifier,
    detail: String? = null,
    subtitle: String? = null,
    onOpen: (() -> Unit)? = null,
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
                    .then(
                        if (onOpen != null || items.isNotEmpty()) {
                            Modifier.contextClickable(
                                onClick = onOpen ?: {},
                                onContextClick = if (items.isNotEmpty()) {
                                    { at ->
                                        pressedAt = at
                                        menu = true
                                    }
                                } else {
                                    null
                                },
                            )
                        } else {
                            Modifier
                        },
                    )
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
                    // Cut at the end: its start is what reads - a user name
                    if (detail != null) {
                        Text(
                            text = detail,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
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
        // down to the press, so its top corner is where the press was. Near an edge it is kept on screen
        ItemMenu(
            expanded = menu,
            onDismiss = { menu = false },
            items = items,
            offset = with(density) { DpOffset(pressedAt.x.toDp(), (pressedAt.y - cardHeight).toDp()) },
        )
    }
}
