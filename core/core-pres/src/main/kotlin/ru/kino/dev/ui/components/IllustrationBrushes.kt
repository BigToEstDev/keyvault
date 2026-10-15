package ru.kino.dev.ui.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/*
 * What the illustrations of empty states are drawn with - the empty folder, the search that found nothing -
 * so they look like one family: the same glow, the same empty sheet, the same sparks.
 */

/** A soft round glow fading out from [center]. */
internal fun DrawScope.drawGlow(color: Color, center: Offset, radius: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = GLOW_ALPHA), Color.Transparent),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}

/**
 * An empty sheet: paper with a dashed edge and dashed lines where text would be.
 *
 * @param lines for each line: where it is, as a share of the canvas height, and how long, as a share of the
 * sheet width
 */
internal fun DrawScope.drawEmptySheet(
    fill: Color,
    line: Color,
    topLeft: Offset,
    sheetSize: Size,
    lines: List<Pair<Float, Float>>,
) {
    val corner = CornerRadius(6.dp.toPx())
    val dash = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))
    val stroke = 1.5.dp.toPx()

    drawRoundRect(color = fill, topLeft = topLeft, size = sheetSize, cornerRadius = corner)
    drawRoundRect(
        color = line,
        topLeft = topLeft,
        size = sheetSize,
        cornerRadius = corner,
        style = Stroke(width = stroke, pathEffect = dash),
    )
    lines.forEach { (y, length) ->
        val start = Offset(topLeft.x + sheetSize.width * 0.18f, size.height * y)
        drawLine(
            color = line,
            start = start,
            end = start + Offset(sheetSize.width * length, 0f),
            strokeWidth = stroke,
            pathEffect = dash,
        )
    }
}

/** A four pointed star. */
internal fun DrawScope.drawSpark(color: Color, center: Offset, radius: Float) {
    val waist = radius * SPARK_WAIST
    val spark = Path().apply {
        moveTo(center.x, center.y - radius)
        lineTo(center.x + waist, center.y - waist)
        lineTo(center.x + radius, center.y)
        lineTo(center.x + waist, center.y + waist)
        lineTo(center.x, center.y + radius)
        lineTo(center.x - waist, center.y + waist)
        lineTo(center.x - radius, center.y)
        lineTo(center.x - waist, center.y - waist)
        close()
    }
    drawPath(spark, color)
}

/** How bright the glow is at its middle. */
/** The size of an illustration when the caller gives none - the empty folder and the empty search alike. */
internal val ILLUSTRATION_WIDTH = 168.dp
internal val ILLUSTRATION_HEIGHT = 140.dp

private const val GLOW_ALPHA = 0.30f

/** How far in the star pinches between its points, as a share of its radius. */
private const val SPARK_WAIST = 0.22f
