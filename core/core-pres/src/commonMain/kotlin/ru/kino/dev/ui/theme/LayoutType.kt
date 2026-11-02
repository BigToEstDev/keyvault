package ru.kino.dev.ui.theme

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * How a screen is laid out - by the width of the window, not by the device: a wide window of a desktop and a tablet
 * on its side are both [Tablet] (pass-docs, Step 94). A screen with layouts of its own picks one by
 * [LocalLayoutType] in one place; the layouts themselves do not ask.
 */
enum class LayoutType {
    /** One column, as on a phone. */
    Mobile,

    /** Room for two side by side. */
    Tablet;

    companion object {
        /** From this width on - [Tablet]. */
        val TABLET_MIN_WIDTH: Dp = 720.dp

        fun of(width: Dp): LayoutType = if (width >= TABLET_MIN_WIDTH) Tablet else Mobile
    }
}

/** The [LayoutType] of the window; `app` provides it at the root ([ProvideLayoutType]). */
val LocalLayoutType = staticCompositionLocalOf { LayoutType.Mobile }

/** Gives [content] the [LayoutType] of the room it has; changes as the window is resized across the line. */
@Composable
fun ProvideLayoutType(content: @Composable () -> Unit) {
    BoxWithConstraints {
        CompositionLocalProvider(LocalLayoutType provides LayoutType.of(maxWidth), content = content)
    }
}
