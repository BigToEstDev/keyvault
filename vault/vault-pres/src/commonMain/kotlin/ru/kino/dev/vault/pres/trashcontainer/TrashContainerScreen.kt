package ru.kino.dev.vault.pres.trashcontainer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import org.orbitmvi.orbit.compose.collectAsState
import ru.kino.dev.pane.koinProcessor
import ru.kino.dev.pane.rememberPaneHost

/**
 * The recycle bin on a wide window - its list and the entries open beside it (pass-docs, Step 100, item 2,
 * `docs/dev/navigation.md`). One layout: the container is the `Tablet` of the bin; on a phone the bin is the list
 * alone.
 */
@Composable
internal fun TrashContainerScreen(
    dbKey: String,
    processor: TrashContainerProcessor = koinProcessor(dbKey),
) {
    val state by processor.collectAsState()
    // The panes open now: those closed are cleared with their processors
    val panes = rememberPaneHost(state.paneKeys)

    processor.TrashContainerTablet(dbKey = dbKey, state = state, panes = panes)
}
