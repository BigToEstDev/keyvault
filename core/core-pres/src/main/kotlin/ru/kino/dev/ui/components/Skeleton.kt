package ru.kino.dev.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified

/**
 * The paint of a skeleton - a shape standing in for what is still being read: a gradient running over it.
 * Colors are the text color of the theme, faint, so it follows light and dark.
 */
@Composable
fun skeletonBrush(): Brush {
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

/**
 * A field while its value is being read: a short bar for the label and the box of the field, painted with
 * [brush] - one brush for every skeleton of a screen keeps them running together.
 */
@Composable
fun SkeletonField(brush: Brush, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .height(LABEL_HEIGHT)
                .fillMaxWidth(LABEL_WIDTH)
                .clip(MaterialTheme.shapes.small)
                .background(brush),
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(FIELD_HEIGHT)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(brush),
        )
    }
}

/**
 * A line of text while it is being read: a bar as tall as the letters of [style], in a box as tall as its
 * line - the row around it keeps the height the text gives it, and nothing moves when the text comes.
 * The bar takes [widthFraction] of the width the box is given.
 */
@Composable
fun SkeletonText(
    brush: Brush,
    style: TextStyle,
    modifier: Modifier = Modifier,
    widthFraction: Float = TEXT_WIDTH,
) {
    val density = LocalDensity.current
    val letters = with(density) { style.fontSize.toDp() }
    val line = if (style.lineHeight.isSpecified) with(density) { style.lineHeight.toDp() } else letters
    Box(modifier = modifier.height(line), contentAlignment = Alignment.CenterStart) {
        Box(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .height(letters)
                .clip(MaterialTheme.shapes.small)
                .background(brush),
        )
    }
}

private val LABEL_HEIGHT = 12.dp

/** Share of the width a line of text takes. */
private const val TEXT_WIDTH = 0.5f

/** The height of a one-line outlined field of Material 3. */
private val FIELD_HEIGHT = 56.dp

/** Share of the width the label bar takes. */
private const val LABEL_WIDTH = 0.3f

private const val DIM = 0.06f
private const val BRIGHT = 0.16f

/** How far the bright band travels, and how wide it is, in px - wider than a phone, so it runs off. */
private const val SWEEP = 1600f
private const val BAND = 500f
private const val SWEEP_MILLIS = 1200
