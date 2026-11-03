package ru.kino.dev.ui.components

import androidx.compose.runtime.Composable
import androidx.activity.compose.BackHandler as ActivityBackHandler

// The back of the activity, as before Compose Multiplatform
@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) = ActivityBackHandler(enabled, onBack)
