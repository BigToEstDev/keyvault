package ru.kino.dev.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Closing the screen in a pane of a container (pass-docs, Step 98, item 8): the cross of its tab. A screen with
 * something to ask before it is closed - a form with changes - registers here ([PaneCloseHandler]); the others the
 * container closes itself.
 */
@Stable
class PaneClose {

    private val handlers = mutableListOf<() -> Unit>()

    /** [handler] registered, the last one answers; the result - what unregisters it. */
    fun register(handler: () -> Unit): () -> Unit {
        handlers += handler
        return { handlers.remove(handler) }
    }

    /** The last handler registered run; false - none: the screen has nothing to ask. */
    fun close(): Boolean {
        val handler = handlers.lastOrNull() ?: return false
        handler()
        return true
    }
}

/** The [PaneClose] of the pane a screen is in; null - not in a pane. */
val LocalPaneClose = staticCompositionLocalOf<PaneClose?> { null }

/** [onClose] - what the screen does when its pane is closed; not in a pane - nothing. */
@Composable
fun PaneCloseHandler(onClose: () -> Unit) {
    val pane = LocalPaneClose.current ?: return
    val current by rememberUpdatedState(onClose)
    DisposableEffect(pane) {
        val unregister = pane.register { current() }
        onDispose(unregister)
    }
}
