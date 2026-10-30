package ru.kino.dev.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset

/**
 * A tap does [onClick]; the gesture of the context does [onContextClick] with where it was, in the bounds of the
 * element - a long press on android, the right button of the mouse on the desktop, and only it (pass-docs, Step 93).
 */
@Composable
expect fun Modifier.contextClickable(onClick: () -> Unit, onContextClick: ((Offset) -> Unit)?): Modifier
