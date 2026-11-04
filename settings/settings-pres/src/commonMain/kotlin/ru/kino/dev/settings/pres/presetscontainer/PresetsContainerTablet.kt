package ru.kino.dev.settings.pres.presetscontainer

import androidx.compose.runtime.Composable
import ru.kino.dev.core.pres.Res
import ru.kino.dev.core.pres.entry_untitled
import ru.kino.dev.core.pres.preset_title_new
import ru.kino.dev.pane.PaneHost
import ru.kino.dev.pane.TabbedContainer
import ru.kino.dev.settings.pres.presetedit.PresetEditScreen
import ru.kino.dev.settings.pres.presets.PresetsScreen
import ru.kino.dev.ui.components.ClosableTab
import ru.kino.dev.ui.res.stringResource

/**
 * The container of the presets ([TabbedContainer]): on the left - their list; on the right, only while a preset is
 * open - the presets open, as tabs, and the form of the one shown. Its cross closes it, not "back".
 */
@Composable
internal fun PresetsContainerActions.PresetsContainerTablet(state: PresetsContainerState, panes: PaneHost) {
    val untitled = stringResource(Res.string.entry_untitled)
    val new = stringResource(Res.string.preset_title_new)
    TabbedContainer(
        tabs = state.tabs.map { tab ->
            val title = when (tab) {
                is PresetTab.Edit -> tab.title?.ifEmpty { untitled }.orEmpty()
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
            state.shown?.let { tab ->
                panes.Pane(key = tab.key, router = tabRouter(tab.key)) {
                    PresetEditScreen(presetId = (tab as? PresetTab.Edit)?.presetId, canGoBack = false)
                }
            }
        },
    )
}
