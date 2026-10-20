package ru.kino.dev.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf

/**
 * What the screens show, as chosen in the settings - `app` provides it at the root, as it does the theme and the
 * language, so the features read it without depending on `settings` (pass-docs, Step 58).
 *
 * @param passwordColoring letters, digits and symbols of a secret shown in clear - each in its own color
 * @param userNameInList the card of an entry has its user name under the title
 */
@Immutable
data class AppDisplay(
    val passwordColoring: Boolean = true,
    val userNameInList: Boolean = false,
)

/** The [AppDisplay] chosen; the defaults until `app` provides one. */
val LocalAppDisplay = compositionLocalOf { AppDisplay() }
