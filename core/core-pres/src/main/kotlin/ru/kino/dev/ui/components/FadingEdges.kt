package ru.kino.dev.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The top and the bottom of a scrolling column fade out where there is more to scroll to - a hint that the
 * list goes on. An edge with nothing beyond it stays sharp. The content is faded, not covered: whatever is
 * behind it - the surface of a menu, a card - shows through, in either theme.
 *
 * Put it before the `verticalScroll` of [state], so it fades the visible part, not the scrolled content.
 */
fun Modifier.verticalFadingEdges(state: ScrollState, length: Dp = FADING_EDGE_LENGTH): Modifier = this
    // Offscreen: the fade cuts into this layer alone, not into what is drawn under it
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val edge = length.toPx().coerceAtMost(size.height / 2)
        // Black keeps the content, transparent takes it away; past its ends a gradient stays at its end color
        if (state.canScrollBackward) {
            drawRect(
                brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black), startY = 0f, endY = edge),
                blendMode = BlendMode.DstIn,
            )
        }
        if (state.canScrollForward) {
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color.Black, Color.Transparent),
                    startY = size.height - edge,
                    endY = size.height,
                ),
                blendMode = BlendMode.DstIn,
            )
        }
    }

private val FADING_EDGE_LENGTH = 24.dp
