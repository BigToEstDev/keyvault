package ru.kino.dev.vault.pres.container

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import org.orbitmvi.orbit.compose.collectAsState
import ru.kino.dev.pane.koinProcessor
import ru.kino.dev.pane.rememberPaneHost
import ru.kino.dev.ui.components.BackHandler

/**
 * The root of `vault` on a wide window - the list and the entries open beside it (pass-docs, Step 98,
 * `docs/dev/navigation.md`). One layout: the container is the `Tablet` of the root; on a phone the root is the list
 * alone.
 */
@Composable
internal fun VaultContainerScreen(
    dbKey: String,
    processor: VaultContainerProcessor = koinProcessor(dbKey),
) {
    val state by processor.collectAsState()
    // The panes open now: those closed are cleared with their processors
    val panes = rememberPaneHost(state.paneKeys)

    // Below the root "back" goes one group up in the list; at the root - the app's, as on a phone
    BackHandler(enabled = state.groups.size > 1, onBack = processor::onGroupUp)

    processor.VaultContainerTablet(dbKey = dbKey, state = state, panes = panes)
}
