package ru.kino.dev.ui.theme

import androidx.compose.runtime.Composable
import java.util.Locale

// Nothing else reads the language on the desktop: the strings of Material 3 are Compose resources too
@Composable
internal actual fun PlatformLocale(locale: Locale?, content: @Composable () -> Unit) = content()

// Before AppLocale sets the default one first: the language and the country the process started with
private val atStart: Locale = Locale.getDefault()

internal actual fun systemLocale(): Locale = atStart
