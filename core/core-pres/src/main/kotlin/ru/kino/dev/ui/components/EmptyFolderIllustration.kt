package ru.kino.dev.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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

/**
 * An open folder with an empty dashed sheet in it, a soft glow behind and a couple of sparks - "nothing
 * here yet", drawn rather than said.
 *
 * Drawn in code, in the colors of the theme, so it follows light and dark and needs no image file. Every
 * coordinate is a share of the canvas, so it scales with [modifier].
 */
@Composable
fun EmptyFolderIllustration(modifier: Modifier = Modifier.size(width = 168.dp, height = 140.dp)) {
    val colors = MaterialTheme.colorScheme
    val folder = colors.primary
    val glow = colors.tertiary
    val sheet = colors.surface
    val sheetLine = colors.onSurfaceVariant

    Canvas(modifier = modifier) {
        drawGlow(glow)
        drawFolderBack(folder.copy(alpha = BACK_ALPHA))
        drawSheet(fill = sheet, line = sheetLine.copy(alpha = SHEET_LINE_ALPHA))
        drawFolderFront(folder)
        drawSpark(glow, center = Offset(size.width * 0.86f, size.height * 0.14f), radius = 9.dp.toPx())
        drawSpark(
            color = glow.copy(alpha = SMALL_SPARK_ALPHA),
            center = Offset(size.width * 0.14f, size.height * 0.20f),
            radius = 6.dp.toPx(),
        )
        drawCircle(
            color = folder.copy(alpha = DOT_ALPHA),
            radius = 3.dp.toPx(),
            center = Offset(size.width * 0.72f, size.height * 0.05f),
        )
    }
}

private fun DrawScope.drawGlow(color: Color) {
    val center = Offset(size.width * 0.5f, size.height * 0.45f)
    val radius = size.minDimension * 0.6f
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

/** The back of the folder, with its tab on top. */
private fun DrawScope.drawFolderBack(color: Color) {
    val corner = CornerRadius(10.dp.toPx())
    drawRoundRect(
        color = color,
        topLeft = Offset(size.width * 0.14f, size.height * 0.22f),
        size = Size(size.width * 0.30f, size.height * 0.16f),
        cornerRadius = corner,
    )
    drawRoundRect(
        color = color,
        topLeft = Offset(size.width * 0.14f, size.height * 0.30f),
        size = Size(size.width * 0.72f, size.height * 0.62f),
        cornerRadius = corner,
    )
}

/** The empty sheet sticking out: paper with a dashed edge and dashed lines where text would be. */
private fun DrawScope.drawSheet(fill: Color, line: Color) {
    val topLeft = Offset(size.width * 0.28f, size.height * 0.16f)
    val sheetSize = Size(size.width * 0.44f, size.height * 0.56f)
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
    listOf(0.28f, 0.36f).forEachIndexed { index, y ->
        val start = Offset(topLeft.x + sheetSize.width * 0.18f, size.height * y)
        val length = sheetSize.width * if (index == 0) 0.64f else 0.42f
        drawLine(
            color = line,
            start = start,
            end = start + Offset(length, 0f),
            strokeWidth = stroke,
            pathEffect = dash,
        )
    }
}

/** The front of the folder, opened towards the viewer: wider at the top than at the bottom. */
private fun DrawScope.drawFolderFront(color: Color) {
    val top = size.height * 0.48f
    val bottom = size.height * 0.92f
    val front = Path().apply {
        moveTo(size.width * 0.08f, top)
        lineTo(size.width * 0.92f, top)
        lineTo(size.width * 0.86f, bottom)
        lineTo(size.width * 0.14f, bottom)
        close()
    }
    drawPath(
        path = front,
        brush = Brush.verticalGradient(
            colors = listOf(color, color.copy(alpha = FRONT_BOTTOM_ALPHA)),
            startY = top,
            endY = bottom,
        ),
    )
}

/** A four pointed star. */
private fun DrawScope.drawSpark(color: Color, center: Offset, radius: Float) {
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

private const val GLOW_ALPHA = 0.30f
private const val BACK_ALPHA = 0.45f
private const val SHEET_LINE_ALPHA = 0.55f
private const val FRONT_BOTTOM_ALPHA = 0.80f
private const val SMALL_SPARK_ALPHA = 0.70f
private const val DOT_ALPHA = 0.50f

/** How far in the star pinches between its points, as a share of its radius. */
private const val SPARK_WAIST = 0.22f
