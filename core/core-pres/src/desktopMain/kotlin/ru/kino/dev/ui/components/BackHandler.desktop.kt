package ru.kino.dev.ui.components

import androidx.compose.runtime.Composable
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState

// The back of navigationevent: the window of compose desktop sends it on Esc
@Composable
internal actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    val state = rememberNavigationEventState(NavigationEventInfo.None)
    NavigationBackHandler(state = state, isBackEnabled = enabled, onBackCompleted = onBack)
}
