package ru.kino.dev.vault.pres.grouplist.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

/**
 * A card while the tree is being read: the shape of a [GroupCard] - the icon, the name - with a gradient
 * running over it. Colors are the text color of the theme, faint, so it follows light and dark.
 */
@Composable
internal fun SkeletonCard(modifier: Modifier = Modifier) {
    val brush = shimmer()

    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(brush),
            )
            Spacer(Modifier.width(24.dp))
            Box(
                modifier = Modifier
                    .height(16.dp)
                    .fillMaxWidth(NAME_WIDTH)
                    .clip(MaterialTheme.shapes.small)
                    .background(brush),
            )
        }
    }
}

@Composable
private fun shimmer(): Brush {
    val base = MaterialTheme.colorScheme.onSurface
    val transition = rememberInfiniteTransition(label = "skeleton")
    val x by transition.animateFloat(
        initialValue = 0f,
        targetValue = SWEEP,
        animationSpec = infiniteRepeatable(tween(SWEEP_MILLIS, easing = LinearEasing), RepeatMode.Restart),
        label = "skeleton-x",
    )
    return Brush.linearGradient(
        colors = listOf(base.copy(alpha = DIM), base.copy(alpha = BRIGHT), base.copy(alpha = DIM)),
        start = Offset(x - BAND, 0f),
        end = Offset(x, 0f),
    )
}

/** Share of the row the name bar takes. */
private const val NAME_WIDTH = 0.6f

private const val DIM = 0.06f
private const val BRIGHT = 0.16f

/** How far the bright band travels, and how wide it is, in px - wider than a phone, so it runs off. */
private const val SWEEP = 1600f
private const val BAND = 500f
private const val SWEEP_MILLIS = 1200
