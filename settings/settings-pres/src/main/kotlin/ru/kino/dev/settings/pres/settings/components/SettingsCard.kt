package ru.kino.dev.settings.pres.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
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

/**
 * A card of the settings screen: an icon, the [title], what is inside in [text] - when there is one - and an
 * arrow. A tap opens the screen behind it with [onClick]; without one the card does not take taps - the
 * screen is not made yet (pass-docs, Step 55). Not [enabled] - a setting to come - it takes no taps and fades as
 * a disabled element of Material 3 does: the text color of the theme at 38% (pass-docs, Step 55, item 2).
 */
@Composable
internal fun SettingsCard(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    text: String? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    if (onClick != null && enabled) {
        Card(onClick = onClick, modifier = modifier.fillMaxWidth()) { CardContent(icon, title, text, enabled) }
    } else {
        Card(modifier = modifier.fillMaxWidth()) { CardContent(icon, title, text, enabled) }
    }
}

@Composable
private fun CardContent(icon: ImageVector, title: String, text: String?, enabled: Boolean) {
    val disabled = MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_ALPHA)
    val main = if (enabled) LocalContentColor.current else disabled
    val secondary = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else disabled

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

// A disabled element of Material 3: the text color at 38%
private const val DISABLED_ALPHA = 0.38f
