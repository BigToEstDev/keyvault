package ru.kino.dev.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp

/**
 * The backdrop of every screen: the background color, two soft glows in opposite corners and a few faint
 * outline shapes. Decoration only - it draws behind [content] and takes no input.
 *
 * Colors come from the theme, so it follows light and dark on its own. What lies on it is painted
 * `onBackground`: the screens put a transparent `Scaffold` over it, which takes the color from here - without
 * it icons with no tint of their own are black, in the dark theme too (pass-docs, Step 56, item 1).
 *
 * On a card ([LocalScreenBackdrop] false) there is no backdrop of its own: [content] lies on the card, in its colors.
 */
@Composable
fun GradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    if (!LocalScreenBackdrop.current) {
        Box(modifier = modifier.fillMaxSize(), content = content)
        return
    }
    val colors = MaterialTheme.colorScheme
    val background = colors.background
    val glowTop = colors.primary.copy(alpha = GLOW_TOP_ALPHA)
    val glowBottom = colors.tertiary.copy(alpha = GLOW_BOTTOM_ALPHA)
    val outline = colors.onBackground.copy(alpha = OUTLINE_ALPHA)

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(background)
                drawGlow(glowTop, center = Offset(size.width * 1.05f, -size.height * 0.02f))
                drawGlow(glowBottom, center = Offset(-size.width * 0.05f, size.height * 1.02f))
                drawOutlines(outline)
            },
    ) {
        CompositionLocalProvider(LocalContentColor provides colors.onBackground) { content() }
    }
}

/**
 * Whether a screen draws its own backdrop - [GradientBackground]: true on its own, false lying on a card - a pane of a
 * container on a wide window, the gradient one for the whole window under the cards (pass-docs, Step 99). The panes
 * of `core-pane` set it; the screens do not ask.
 */
val LocalScreenBackdrop = staticCompositionLocalOf { true }

private fun DrawScope.drawGlow(color: Color, center: Offset) {
    val radius = size.minDimension * GLOW_RADIUS
    drawCircle(
        brush = Brush.radialGradient(listOf(color, Color.Transparent), center = center, radius = radius),
        radius = radius,
        center = center,
    )
}

private fun DrawScope.drawOutlines(color: Color) {
    val stroke = Stroke(width = 1.dp.toPx())

    val square = 80.dp.toPx()
    val squareTopLeft = Offset(size.width * 0.92f - square, size.height * 0.10f)
    rotate(degrees = 12f, pivot = squareTopLeft + Offset(square / 2, square / 2)) {
        drawRoundRect(
            color = color,
            topLeft = squareTopLeft,
            size = Size(square, square),
            cornerRadius = CornerRadius(16.dp.toPx()),
            style = stroke,
        )
    }

    val circle = 32.dp.toPx()
    drawCircle(
        color = color,
        radius = circle,
        center = Offset(size.width * 0.08f + circle, size.height * 0.88f - circle),
        style = stroke,
    )

    val ring = 12.dp.toPx()
    drawCircle(
        color = color,
        radius = ring,
        center = Offset(size.width * 0.18f, size.height * 0.22f),
        style = stroke,
    )
}

private const val GLOW_TOP_ALPHA = 0.25f
private const val GLOW_BOTTOM_ALPHA = 0.15f
private const val OUTLINE_ALPHA = 0.08f

/** Glow radius as a share of the shorter side of the screen. */
private const val GLOW_RADIUS = 0.9f
