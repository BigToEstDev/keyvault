package ru.kino.dev.settings.pres.presetscontainer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import ru.kino.dev.navigation.PresetEditRoute
import ru.kino.dev.pane.koinProcessor
import ru.kino.dev.pane.rememberPaneHost

/**
 * The presets on a wide window - their list and the presets open beside it (pass-docs, Step 100, item 3,
 * `docs/dev/navigation.md`). One layout: the container is the `Tablet` of the presets and of a preset; on a phone each
 * is a screen alone.
 *
 * @param opening the preset it is opened with - "a new preset" from the form of an entry; null - none
 */
@Composable
internal fun PresetsContainerScreen(
    opening: PresetEditRoute? = null,
    processor: PresetsContainerProcessor = koinProcessor(opening),
) {
    val state by processor.collectAsState()
    // The panes open now: those closed are cleared with their processors
    val panes = rememberPaneHost(state.paneKeys)

    processor.collectSideEffect {
        when (it) {
            is PresetsContainerSideEffect.ClosePane -> panes.close(it.pane)
        }
    }

    processor.PresetsContainerTablet(state = state, panes = panes)
}
