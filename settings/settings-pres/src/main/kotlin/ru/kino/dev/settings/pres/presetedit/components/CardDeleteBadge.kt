package ru.kino.dev.settings.pres.presetedit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import ru.kino.dev.ui.theme.DISABLED_ALPHA
import ru.kino.dev.ui.theme.DISABLED_CONTAINER_ALPHA

/**
 * The red cross that deletes a card the person added - a round badge over the card's top end corner, on its
 * outline, not in its body (pass-docs, Step 40, fix 18.1). Place it at the card's top end: it moves itself onto
 * the corner. The badge is small, the touch is of a full button around it.
 *
 * Not [enabled] - while the preset is saved - it fades as a disabled button of Material 3 does.
 */
@Composable
internal fun CardDeleteBadge(
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            // From the top end of the card out, so the badge's center lies on the rounding of the corner
            .offset(x = CORNER_OFFSET, y = -CORNER_OFFSET)
            .size(TOUCH_SIZE)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = null,
                indication = ripple(bounded = false, radius = BADGE_SIZE),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(BADGE_SIZE)
                .background(
                    if (enabled) colors.error else colors.onSurface.copy(alpha = DISABLED_CONTAINER_ALPHA),
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Close,
                contentDescription = contentDescription,
                tint = if (enabled) colors.onError else colors.onSurface.copy(alpha = DISABLED_ALPHA),
                modifier = Modifier.size(ICON_SIZE),
            )
        }
    }
}

private val BADGE_SIZE = 24.dp
private val ICON_SIZE = 16.dp
private val TOUCH_SIZE = 48.dp

/** Half the touch less a few - the badge's center sits on the rounded corner, a little in from its point. */
private val CORNER_OFFSET = 20.dp
