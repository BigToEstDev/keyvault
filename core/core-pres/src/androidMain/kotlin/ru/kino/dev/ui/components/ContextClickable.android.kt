package ru.kino.dev.ui.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput

/** The long press - where the finger went down, seen apart: it consumes nothing, so the click works. */
@Composable
actual fun Modifier.contextClickable(onClick: () -> Unit, onContextClick: ((Offset) -> Unit)?): Modifier {
    var pressedAt by remember { mutableStateOf(Offset.Zero) }
    return this
        .pointerInput(Unit) {
            awaitEachGesture { pressedAt = awaitFirstDown(requireUnconsumed = false).position }
        }
        .combinedClickable(
            onClick = onClick,
            onLongClick = onContextClick?.let { { it(pressedAt) } },
        )
}
