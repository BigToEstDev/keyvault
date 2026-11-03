package ru.kino.dev.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState

/**
 * "Back" of the platform while [enabled]: the system back on android, Esc on the desktop. One for every screen that
 * asks before leaving (pass-docs, Step 88). In a pane of a container it is also the "back" of the pane - [PaneBack],
 * the cross of its tab (Step 98, item 6).
 */
@Composable
fun BackHandler(enabled: Boolean = true, onBack: () -> Unit) {
    PlatformBackHandler(enabled, onBack)
    val pane = LocalPaneBack.current
    val current by rememberUpdatedState(onBack)
    DisposableEffect(pane, enabled) {
        val unregister = if (pane != null && enabled) pane.register { current() } else null
        onDispose { unregister?.invoke() }
    }
}

@Composable
internal expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)
