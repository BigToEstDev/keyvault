package ru.kino.dev.ui.components

import androidx.compose.runtime.Composable

// No autofill on the desktop
@Composable
internal actual fun ExcludeWindowFromAutofill() = Unit
