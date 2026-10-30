package ru.kino.dev.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput

/** The right button: on its press, where the mouse is. A long press of the left one is only a click. */
@Composable
actual fun Modifier.contextClickable(onClick: () -> Unit, onContextClick: ((Offset) -> Unit)?): Modifier {
    val context by rememberUpdatedState(onContextClick)
    return this
        .pointerInput(Unit) {
            awaitEachGesture {
                val event = awaitPointerEvent()
                val onContext = context
                if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed && onContext != null) {
                    event.changes.forEach { it.consume() }
                    onContext(event.changes.first().position)
                }
            }
        }
        .clickable(onClick = onClick)
}
