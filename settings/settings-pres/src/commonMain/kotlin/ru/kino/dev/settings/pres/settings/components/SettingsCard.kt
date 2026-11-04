package ru.kino.dev.settings.pres.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.kino.dev.ui.theme.DISABLED_ALPHA

/**
 * A card of the settings screen: an icon, the [title], what is inside in [text] - when there is one - and an
 * arrow. A tap opens the screen behind it with [onClick]; without one the card does not take taps - the
 * screen is not made yet (pass-docs, Step 55). Not [enabled] - a setting to come - it takes no taps and fades as
 * a disabled element of Material 3 does: the text color of the theme at 38% (pass-docs, Step 55, item 2).
 * [selected] - its screen is the one open beside the list, on a wide window (pass-docs, Step 101).
 */
@Composable
internal fun SettingsCard(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    text: String? = null,
    enabled: Boolean = true,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val colors = if (selected) {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    } else {
        CardDefaults.cardColors()
    }
    if (onClick != null && enabled) {
        Card(onClick = onClick, modifier = modifier.fillMaxWidth(), colors = colors) {
            CardContent(icon, title, text, enabled, selected)
        }
    } else {
        Card(modifier = modifier.fillMaxWidth(), colors = colors) { CardContent(icon, title, text, enabled, selected) }
    }
}

@Composable
private fun CardContent(icon: ImageVector, title: String, text: String?, enabled: Boolean, selected: Boolean) {
    val disabled = MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_ALPHA)
    val main = if (enabled) LocalContentColor.current else disabled
    val secondary = when {
        !enabled -> disabled
        selected -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = main)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 24.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = main,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (text != null) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = secondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = secondary,
        )
    }
}
