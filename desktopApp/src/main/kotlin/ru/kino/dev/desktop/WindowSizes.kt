package ru.kino.dev.desktop

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import java.awt.Dimension
import java.awt.GraphicsEnvironment

/**
 * The size of the window (pass-docs, Step 90): opens made for the desktop, the layout follows it; never narrower than
 * the layout of the phone. Not remembered between runs - `plan/todo/claude-suggestions.md`.
 */
internal object WindowSizes {

    /** Under it the layout of the phone breaks: made for 360 wide. */
    val Min = DpSize(360.dp, 600.dp)

    /** Fits a laptop of 1366 x 768 with its taskbar. */
    private val Default = DpSize(1000.dp, 720.dp)

    /** [Min] for AWT - its sizes are in the same units as dp, the scale of the screen applied. */
    val minAwt: Dimension
        get() = Dimension(Min.width.value.toInt(), Min.height.value.toInt())

    /** The size to open with on the screen of [screen] - the space it leaves to windows: cut to it, never under [Min]. */
    fun initial(screen: DpSize): DpSize = DpSize(
        width = Default.width.coerceAtMost(screen.width).coerceAtLeast(Min.width),
        height = Default.height.coerceAtMost(screen.height).coerceAtLeast(Min.height),
    )

    /** The space the main screen leaves to windows - without the taskbar and the like. */
    fun screen(): DpSize {
        val bounds = GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds
        return DpSize(bounds.width.dp, bounds.height.dp)
    }
}
