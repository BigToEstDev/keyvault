package ru.kino.dev.ui.components

import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The "back" of the screen in a pane of a container (pass-docs, Step 98, item 6): the [BackHandler]s of the screen
 * register here, the container asks it to go back - the cross of a tab, as "back" on a phone: a form with changes asks
 * first.
 */
@Stable
class PaneBack {

    private val handlers = mutableListOf<() -> Unit>()

    /** [handler] registered, the last one answers; the result - what unregisters it. */
    fun register(handler: () -> Unit): () -> Unit {
        handlers += handler
        return { handlers.remove(handler) }
    }

    /** The last handler registered run; false - none: the screen does not ask before leaving. */
    fun handle(): Boolean {
        val handler = handlers.lastOrNull() ?: return false
        handler()
        return true
    }
}

/** The [PaneBack] of the pane a screen is in; null - not in a pane. */
val LocalPaneBack = staticCompositionLocalOf<PaneBack?> { null }
