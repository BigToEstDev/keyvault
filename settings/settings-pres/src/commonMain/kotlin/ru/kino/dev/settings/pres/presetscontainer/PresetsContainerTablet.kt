package ru.kino.dev.settings.pres.presetscontainer

import androidx.compose.runtime.Composable
import ru.kino.dev.core.pres.Res
import ru.kino.dev.core.pres.entry_untitled
import ru.kino.dev.core.pres.preset_title_new
import ru.kino.dev.core.preset.StandardPreset
import ru.kino.dev.pane.PaneHost
import ru.kino.dev.pane.TabbedContainer
import ru.kino.dev.settings.pres.presetedit.PresetEditScreen
import ru.kino.dev.settings.pres.presetread.PresetReadScreen
import ru.kino.dev.settings.pres.presets.PresetsScreen
import ru.kino.dev.ui.components.ClosableTab
import ru.kino.dev.ui.preset.presetName
import ru.kino.dev.ui.res.stringResource

/**
 * The container of the presets ([TabbedContainer]): on the left - their list; on the right, only while a preset is
 * open - the presets open, as tabs, and the page of the one shown: its reading or its form (pass-docs, Step 99,
 * item 8). Its cross closes it, not "back".
 */
@Composable
internal fun PresetsContainerActions.PresetsContainerTablet(state: PresetsContainerState, panes: PaneHost) {
    val untitled = stringResource(Res.string.entry_untitled)
    val new = stringResource(Res.string.preset_title_new)
    val standard = presetName(StandardPreset.preset)
    TabbedContainer(
        tabs = state.tabs.map { tab ->
            val title = when (tab) {
                is PresetTab.Preset ->
                    if (tab.presetId == null) standard else tab.title?.ifEmpty { untitled }.orEmpty()
                is PresetTab.New -> new
            }
            ClosableTab(tab.key, title)
        },
        selected = state.shown?.key,
        onSelect = ::onSelectTab,
        onClose = ::onCloseTab,
        list = {
            panes.Pane(key = PRESETS_LIST_PANE, router = listRouter) {
                PresetsScreen(drafts = state.drafts)
            }
        },
        page = {
            when (val tab = state.shown) {
                null -> Unit

                is PresetTab.Preset -> panes.Pane(key = tab.pane, router = presetRouter(tab.presetId, tab.editing)) {
                    if (tab.editing) {
                        PresetEditScreen(presetId = tab.presetId, canGoBack = false)
                    } else {
                        PresetReadScreen(presetId = tab.presetId, canGoBack = false)
                    }
                }

                is PresetTab.New -> panes.Pane(key = tab.pane, router = newRouter(tab.key)) {
                    PresetEditScreen(presetId = null, readSaved = tab.readSaved, canGoBack = false)
                }
            }
        },
    )
}
