package ru.kino.dev.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf

/**
 * What the screens show, as chosen in the settings - `app` provides it at the root, as it does the theme and the
 * language, so the features read it without depending on `settings` (pass-docs, Step 58).
 *
 * @param passwordColoring letters, digits and symbols of a secret shown in clear - each in its own color
 * @param userNameInList the card of an entry has its user name under the title
 * @param entryCount the icon of a group has the number of its entries on a badge
 * @param entryCountNested that number counts the entries of the groups below too, not only its own
 * @param lockButton the screens of the open database have the button that locks it now (Step 59)
 * @param presets the screens of an entry have the choice of presets; off - a new entry takes the standard one, one
 *   saved keeps its own (Step 62)
 */
@Immutable
data class AppDisplay(
    val passwordColoring: Boolean = true,
    val userNameInList: Boolean = false,
    val entryCount: Boolean = true,
    val entryCountNested: Boolean = true,
    val lockButton: Boolean = false,
    val presets: Boolean = true,
)

/** The [AppDisplay] chosen; the defaults until `app` provides one. */
val LocalAppDisplay = compositionLocalOf { AppDisplay() }
