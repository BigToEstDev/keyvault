package ru.kino.dev.vault.pres.grouplist.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import ru.kino.dev.ui.theme.LocalAppDisplay
import ru.kino.dev.vault.group.EntryCount

/**
 * The number the badge of a group shows, as the settings say: null - no number; otherwise the entries of the
 * groups below too, or only its own (pass-docs, Step 58).
 */
@Composable
@ReadOnlyComposable
internal fun EntryCount.shown(): Int? {
    val display = LocalAppDisplay.current
    return when {
        !display.entryCount -> null
        display.entryCountNested -> nested
        else -> direct
    }
}
