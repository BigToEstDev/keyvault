package ru.kino.dev.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * A magnifier over an empty dashed sheet, a little glow behind and sparks for an accent - "nothing found",
 * drawn rather than said. The sibling of [EmptyFolderIllustration]: the same brushes, size and colors.
 *
 * Drawn in code with the brushes of `IllustrationBrushes.kt`, in the colors of the theme, so it follows
 * light and dark and needs no image file. Every coordinate is a share of the canvas, so it scales with
 * [modifier].
 */
@Composable
fun EmptySearchIllustration(modifier: Modifier = Modifier.size(width = 168.dp, height = 140.dp)) {
    val colors = MaterialTheme.colorScheme
    val magnifier = colors.primary
    val glow = colors.tertiary
    val sheet = colors.surface
    val sheetLine = colors.onSurfaceVariant

    Canvas(modifier = modifier) {
        drawGlow(glow, center = Offset(size.width * 0.5f, size.height * 0.5f), radius = size.minDimension * 0.6f)
        drawEmptySheet(
            fill = sheet,
            line = sheetLine.copy(alpha = SHEET_LINE_ALPHA),
            topLeft = Offset(size.width * 0.20f, size.height * 0.10f),
            sheetSize = Size(size.width * 0.46f, size.height * 0.70f),
            lines = listOf(0.24f to 0.64f, 0.32f to 0.50f, 0.40f to 0.34f),
        )
        drawMagnifier(
            color = magnifier,
            glass = sheet,
            center = Offset(size.width * 0.60f, size.height * 0.56f),
            radius = size.minDimension * 0.17f,
        )
        drawSpark(glow, center = Offset(size.width * 0.86f, size.height * 0.16f), radius = 9.dp.toPx())
        drawSpark(
            color = glow.copy(alpha = SMALL_SPARK_ALPHA),
            center = Offset(size.width * 0.10f, size.height * 0.30f),
            radius = 6.dp.toPx(),
        )
        drawCircle(
            color = magnifier.copy(alpha = DOT_ALPHA),
            radius = 3.dp.toPx(),
            center = Offset(size.width * 0.76f, size.height * 0.06f),
        )
    }
}

/** A round lens with a thick rim and a handle going down to the right, a glint on the glass. */
private fun DrawScope.drawMagnifier(color: Color, glass: Color, center: Offset, radius: Float) {
    val rim = 6.dp.toPx()
    // Down to the right, at 45 degrees: from just outside the rim, as long as the lens is wide
    val diagonal = Offset(DIAGONAL, DIAGONAL)
    val handleStart = center + diagonal * (radius + rim / 2)
    val handleEnd = handleStart + diagonal * (radius * HANDLE_LENGTH)

    drawLine(color = color, start = handleStart, end = handleEnd, strokeWidth = 9.dp.toPx(), cap = StrokeCap.Round)
    drawCircle(color = glass.copy(alpha = GLASS_ALPHA), radius = radius, center = center)
    drawCircle(color = color, radius = radius, center = center, style = Stroke(width = rim))
    drawArc(
        color = color.copy(alpha = GLINT_ALPHA),
        startAngle = 200f,
        sweepAngle = 60f,
        useCenter = false,
        topLeft = center - Offset(radius * GLINT_RADIUS, radius * GLINT_RADIUS),
        size = Size(radius * GLINT_RADIUS * 2, radius * GLINT_RADIUS * 2),
        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
    )
}

private const val SHEET_LINE_ALPHA = 0.55f
private const val SMALL_SPARK_ALPHA = 0.70f
private const val DOT_ALPHA = 0.50f

/** The glass lets the sheet behind show through a little. */
private const val GLASS_ALPHA = 0.85f
private const val GLINT_ALPHA = 0.45f

/** The glint runs inside the rim, at this share of the lens radius. */
private const val GLINT_RADIUS = 0.62f

/** The handle, as a share of the lens radius. */
private const val HANDLE_LENGTH = 1.1f

/** cos 45° - the handle goes down to the right at that angle. */
private const val DIAGONAL = 0.7071f
