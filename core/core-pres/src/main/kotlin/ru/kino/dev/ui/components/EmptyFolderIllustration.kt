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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp

/**
 * An open folder with an empty dashed sheet in it, a soft glow behind and a couple of sparks - "nothing
 * here yet", drawn rather than said.
 *
 * Drawn in code with the brushes of `IllustrationBrushes.kt`, in the colors of the theme, so it follows
 * light and dark and needs no image file. Every coordinate is a share of the canvas, so it scales with
 * [modifier]; without a size in it - [ILLUSTRATION_WIDTH] by [ILLUSTRATION_HEIGHT].
 */
@Composable
fun EmptyFolderIllustration(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val folder = colors.primary
    val glow = colors.tertiary
    val sheet = colors.surface
    val sheetLine = colors.onSurfaceVariant

    // A size given in [modifier] comes first and wins over this one
    Canvas(modifier = modifier.size(width = ILLUSTRATION_WIDTH, height = ILLUSTRATION_HEIGHT)) {
        drawGlow(glow, center = Offset(size.width * 0.5f, size.height * 0.45f), radius = size.minDimension * 0.6f)
        drawFolderBack(folder.copy(alpha = BACK_ALPHA))
        drawEmptySheet(
            fill = sheet,
            line = sheetLine.copy(alpha = SHEET_LINE_ALPHA),
            topLeft = Offset(size.width * 0.28f, size.height * 0.16f),
            sheetSize = Size(size.width * 0.44f, size.height * 0.56f),
            lines = listOf(0.28f to 0.64f, 0.36f to 0.42f),
        )
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

private const val BACK_ALPHA = 0.45f
private const val SHEET_LINE_ALPHA = 0.55f
private const val FRONT_BOTTOM_ALPHA = 0.80f
private const val SMALL_SPARK_ALPHA = 0.70f
private const val DOT_ALPHA = 0.50f
