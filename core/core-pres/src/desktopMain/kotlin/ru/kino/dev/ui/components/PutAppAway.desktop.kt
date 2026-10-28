package ru.kino.dev.ui.components

import androidx.compose.runtime.Composable
import java.awt.Frame
import java.awt.KeyboardFocusManager

// The window "back" came from is the active one: minimized
@Composable
actual fun rememberPutAppAway(): () -> Unit = {
    (KeyboardFocusManager.getCurrentKeyboardFocusManager().activeWindow as? Frame)?.extendedState = Frame.ICONIFIED
}
