package ru.kino.dev.pane

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ru.kino.dev.ui.theme.LayoutType
import ru.kino.dev.ui.theme.LocalLayoutType

/**
 * [onChange] once the window is resized across the line of [LayoutType] - not as it is first shown. The back stack of
 * one layout is not the other's: a container on a wide window holds in itself what a phone keeps as places on the
 * stack, so the app folds the stack back to the container's place (pass-docs, `docs/dev/navigation.md`, Step 98).
 */
@Composable
fun LayoutChangeEffect(onChange: (LayoutType) -> Unit) {
    val layout = LocalLayoutType.current
    var shown by remember { mutableStateOf(layout) }
    LaunchedEffect(layout) {
        if (layout != shown) {
            shown = layout
            onChange(layout)
        }
    }
}
