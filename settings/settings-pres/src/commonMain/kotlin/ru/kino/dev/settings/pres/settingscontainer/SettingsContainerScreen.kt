package ru.kino.dev.settings.pres.settingscontainer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import org.orbitmvi.orbit.compose.collectAsState
import ru.kino.dev.pane.koinProcessor
import ru.kino.dev.pane.rememberPaneHost

/**
 * The settings of [dbKey] on a wide window - the main screen and the page chosen beside it (pass-docs, Step 101,
 * `docs/dev/navigation.md`). One layout: the container is the `Tablet` of the settings; on a phone each is a screen
 * alone.
 */
@Composable
internal fun SettingsContainerScreen(
    dbKey: String,
    processor: SettingsContainerProcessor = koinProcessor(dbKey),
) {
    val state by processor.collectAsState()
    // The panes open now: the page left is cleared with its processor
    val panes = rememberPaneHost(state.paneKeys)

    processor.SettingsContainerTablet(dbKey = dbKey, state = state, panes = panes)
}
