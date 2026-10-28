package ru.kino.dev.ui.components

import androidx.compose.runtime.Composable

/**
 * "Back" of the platform while [enabled]: the system back on android, Esc on the desktop. One for every screen that
 * asks before leaving (pass-docs, Step 88).
 */
@Composable
expect fun BackHandler(enabled: Boolean = true, onBack: () -> Unit)
