package ru.kino.dev.ui.components

import androidx.compose.runtime.Composable

/**
 * Puts the app away, as "back" does on the lock screen - going back would show the open database under the lock
 * (`LockedScreen` of welcome): the task to the back on android, the window minimized on the desktop.
 */
@Composable
expect fun rememberPutAppAway(): () -> Unit
