package ru.kino.dev.welcome.pres.common

import androidx.compose.foundation.Image
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * The picture of the app: a shield with a keyhole - on the welcome screen, and over the password on the
 * unlock and the locked screens. Decoration - no content description.
 *
 * Built from the theme colors rather than drawn in fixed ones, so it follows light and dark.
 */
@Composable
internal fun ShieldImage(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val image = remember(colors.primary, colors.onPrimary, colors.onBackground) {
        shieldWithKeyhole(shield = colors.primary, shade = colors.onBackground, keyhole = colors.onPrimary)
    }
    Image(imageVector = image, contentDescription = null, modifier = modifier)
}

private fun shieldWithKeyhole(shield: Color, shade: Color, keyhole: Color): ImageVector =
    ImageVector.Builder(
        name = "ShieldWithKeyhole",
        defaultWidth = 120.dp,
        defaultHeight = 120.dp,
        viewportWidth = 120f,
        viewportHeight = 120f,
    ).apply {
        // The shield
        path(fill = SolidColor(shield)) {
            moveTo(60f, 6f)
            lineTo(104f, 22f)
            verticalLineTo(56f)
            curveTo(104f, 84f, 86f, 104f, 60f, 114f)
            curveTo(34f, 104f, 16f, 84f, 16f, 56f)
            verticalLineTo(22f)
            close()
        }
        // Its right half a shade darker, for depth
        path(fill = SolidColor(shade), fillAlpha = SHADE_ALPHA) {
            moveTo(60f, 6f)
            lineTo(104f, 22f)
            verticalLineTo(56f)
            curveTo(104f, 84f, 86f, 104f, 60f, 114f)
            close()
        }
        // The keyhole: a round head and a tapering stem
        path(fill = SolidColor(keyhole)) {
            moveTo(47f, 50f)
            arcToRelative(13f, 13f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = 26f, dy1 = 0f)
            arcToRelative(13f, 13f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = -26f, dy1 = 0f)
            close()
            moveTo(55f, 58f)
            lineTo(51f, 84f)
            horizontalLineTo(69f)
            lineTo(65f, 58f)
            close()
        }
    }.build()

private const val SHADE_ALPHA = 0.12f
