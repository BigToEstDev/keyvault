package ru.kino.dev.ui.components

import androidx.compose.runtime.Composable
import androidx.activity.compose.BackHandler as ActivityBackHandler

// The back of the activity, as before Compose Multiplatform
@Composable
internal actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) = ActivityBackHandler(enabled, onBack)
