package ru.kino.dev.ui.components

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable

@Composable
actual fun rememberPutAppAway(): () -> Unit {
    val activity = LocalActivity.current
    return { activity?.moveTaskToBack(true) }
}
