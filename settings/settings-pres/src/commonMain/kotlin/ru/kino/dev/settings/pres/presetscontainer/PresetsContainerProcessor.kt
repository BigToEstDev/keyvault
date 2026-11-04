package ru.kino.dev.settings.pres.presetscontainer

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.first
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ru.kino.dev.core.preset.EntryPreset
import ru.kino.dev.core.preset.PresetRepository
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.PresetEditRoute
import ru.kino.dev.pane.PaneRouter
import ru.kino.dev.pane.closeTab
import ru.kino.dev.settings.pres.navigation.PresetReadRoute

/**
 * The container of the presets on a wide window (pass-docs, Step 100, item 3, `docs/dev/navigation.md`): the list of
 * the presets on the left, the presets open on the right as tabs. The screens in it are those of a phone, each with its
 * own processor; what they navigate to that is the container's comes here through [listRouter], [presetRouter] and
 * [newRouter], the rest goes on to the navigation of the app - "back" of the list too: back to the settings.
 *
 * A tab is one page at a time, as a tab of an entry of `vault` (Step 99, item 8): the preset read, or its form over the
 * reading; a new preset is made in a tab of its own and, made from the list, read in it once saved.
 *
 * In the place of the presets on the back stack it opens with none; in the place of a preset - "a new preset" from
 * the form of an entry - with its tab, [opening]: leaving that one - saved or not - leaves the container, back to the
 * form, as on a phone.
 *
 * The title of a tab is the name of its preset, as the presets are now: renamed - the title follows.
 */
class PresetsContainerProcessor(
    opening: PresetEditRoute?,
    private val presets: PresetRepository,
) : ViewModel(),
    OrbitContainerHost<PresetsContainerState, PresetsContainerState, PresetsContainerSideEffect>,
    PresetsContainerActions {

    /** The new presets made in the container so far - the tab of each its own key. */
    private var made = 0

    /** The tab the container was opened with; leaving it leaves the container. */
    private val origin: PresetTab? = opening?.let(::tabFor)

    override val container = orbitContainer<PresetsContainerState, PresetsContainerSideEffect>(
        PresetsContainerState(tabs = listOfNotNull(origin), active = origin?.key),
    ) {
        // While the container is shown: the titles of the tabs follow the presets
        repeatOnSubscription {
            presets.presets.collect { own -> reduce { state.named(own) } }
        }
    }

    override val listRouter = PaneRouter { event ->
        val route = (event as? MviNavEvent.NavigateTo)?.route
        when (route) {
            is PresetReadRoute -> open(PresetTab.Preset(route.presetId))
            is PresetEditRoute -> open(tabFor(route))
            else -> return@PaneRouter false
        }
        true
    }

    /** Only from the page shown: one under it by now - nothing changes, and the event is still the container's. */
    override fun presetRouter(presetId: String?, editing: Boolean) = PaneRouter { event ->
        val key = presetPane(presetId)
        val change: (PresetsContainerState.() -> PresetsContainerState)? = when {
            event == MviNavEvent.NavigateBack ->
                if (editing) { { withPreset(key) { it.copy(editing = false) } } } else { { closed(key) } }

            event is MviNavEvent.NavigateTo && event.route is PresetEditRoute && !editing ->
                { { withPreset(key) { it.copy(editing = true) } } }

            event is MviNavEvent.NavigateBackTo && event.popUpTo is PresetReadRoute ->
                if (event.inclusive) { { closed(key) } } else { { withPreset(key) { it.copy(editing = false) } } }

            else -> null
        }
        if (change != null) {
            intent { reduce { if (state.preset(key)?.editing == editing) state.change() else state } }
        }
        change != null
    }

    override fun newRouter(key: String) = PaneRouter { event ->
        when {
            // Back to the form of the entry it came from
            event == MviNavEvent.NavigateBack && key == origin?.key -> false

            event == MviNavEvent.NavigateBack -> {
                intent { reduce { state.closed(key) } }
                true
            }

            event is MviNavEvent.NavigateTo && event.route is PresetReadRoute -> {
                val presetId = (event.route as PresetReadRoute).presetId
                intent {
                    reduce { state.made(key, presetId) }
                    val own = presets.presets.first()
                    reduce { state.named(own) }
                }
                true
            }

            else -> false
        }
    }

    override fun onSelectTab(key: String) {
        intent { reduce { if (state.tabs.any { it.key == key }) state.copy(active = key) else state } }
    }

    override fun onCloseTab(key: String) {
        intent {
            val tab = state.tabs.firstOrNull { it.key == key } ?: return@intent
            if (tab is PresetTab.New || (tab is PresetTab.Preset && tab.editing)) {
                // A form: shown, so it can ask; it goes away itself, through its router
                reduce { state.copy(active = key) }
                postSideEffect(PresetsContainerSideEffect.ClosePane(tab.pane))
            } else {
                // The reading asks nothing
                reduce { state.closed(key) }
            }
        }
    }

    /**
     * [tab] opened from the list: a preset open already - shown, its form over its reading if the form is asked for -
     * one there already keeps its changes; a new one - a tab of its own.
     */
    private fun open(tab: PresetTab) {
        intent {
            val open = state.preset(tab.key)
            reduce {
                val tabs = when {
                    open == null -> state.tabs + tab
                    tab is PresetTab.Preset && tab.editing -> state.tabs.map { if (it.key == tab.key) open.copy(editing = true) else it }
                    else -> state.tabs
                }
                state.copy(tabs = tabs, active = tab.key)
            }
            if (open == null && tab is PresetTab.Preset) {
                val own = presets.presets.first()
                reduce { state.named(own) }
            }
        }
    }

    /** A preset of one's own - its form; null - a new one. The title of a preset comes from the presets, as read. */
    private fun tabFor(route: PresetEditRoute): PresetTab =
        route.presetId?.let { PresetTab.Preset(it, editing = true) } ?: PresetTab.New(newPresetTab(++made), route.readSaved)

    private fun PresetsContainerState.preset(key: String): PresetTab.Preset? =
        tabs.firstOrNull { it.key == key } as? PresetTab.Preset

    private fun PresetsContainerState.withPreset(key: String, change: (PresetTab.Preset) -> PresetTab.Preset) =
        copy(tabs = tabs.map { if (it is PresetTab.Preset && it.key == key) change(it) else it })

    /** The new preset of the tab [key] made - [presetId]: the tab reads it, in the same place; shown, if it was. */
    private fun PresetsContainerState.made(key: String, presetId: String?): PresetsContainerState {
        if (tabs.none { it.key == key }) return this
        val read = PresetTab.Preset(presetId)
        return copy(tabs = tabs.map { if (it.key == key) read else it }, active = if (active == key) read.key else active)
    }

    /** The tabs of the presets titled by their names in [own]; a preset not there keeps the title it had. */
    private fun PresetsContainerState.named(own: List<EntryPreset>): PresetsContainerState {
        val names = own.mapNotNull { preset -> preset.id?.let { it.value to preset.name.orEmpty() } }.toMap()
        return copy(
            tabs = tabs.map { tab ->
                if (tab is PresetTab.Preset && tab.presetId != null) tab.copy(title = names[tab.presetId] ?: tab.title) else tab
            },
        )
    }

    /** The tab [key] gone; shown instead - the next one, else the one before. */
    private fun PresetsContainerState.closed(key: String): PresetsContainerState =
        closeTab(tabs, active, key, PresetTab::key)?.let { copy(tabs = it.tabs, active = it.active) } ?: this
}
