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

/**
 * The container of the presets on a wide window (pass-docs, Step 100, item 3, `docs/dev/navigation.md`): the list of
 * the presets on the left, the presets open on the right as tabs - each its form; as the container of the recycle
 * bin. The screens in it are those of a phone, each with its own processor; what they navigate to that is the
 * container's comes here through [listRouter] and [tabRouter], the rest goes on to the navigation of the app - "back"
 * of the list too: back to the settings.
 *
 * In the place of the presets on the back stack it opens with none; in the place of a preset - "a new preset" from
 * the form of an entry - with its tab, [opening]: leaving that one - saved or not - leaves the container, back to the
 * form, as on a phone. The other tabs close.
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
        if (event is MviNavEvent.NavigateTo && event.route is PresetEditRoute) {
            open(event.route as PresetEditRoute)
            true
        } else {
            false
        }
    }

    override fun tabRouter(key: String) = PaneRouter { event ->
        when {
            event != MviNavEvent.NavigateBack -> false
            // Back to the form of the entry it came from
            key == origin?.key -> false
            else -> {
                intent { reduce { state.closed(key) } }
                true
            }
        }
    }

    override fun onSelectTab(key: String) {
        intent { reduce { if (state.tabs.any { it.key == key }) state.copy(active = key) else state } }
    }

    override fun onCloseTab(key: String) {
        intent {
            if (state.tabs.none { it.key == key }) return@intent
            // Every tab is a form: shown, so it can ask; it goes away itself, through its router
            reduce { state.copy(active = key) }
            postSideEffect(PresetsContainerSideEffect.ClosePane(key))
        }
    }

    /** A preset open already - shown; a new one - a tab of its own. */
    private fun open(route: PresetEditRoute) {
        intent {
            val open = route.presetId?.let { id -> state.tabs.firstOrNull { it is PresetTab.Edit && it.presetId == id } }
            val tab = open ?: tabFor(route)
            reduce { state.copy(tabs = if (open != null) state.tabs else state.tabs + tab, active = tab.key) }
            if (open == null && tab is PresetTab.Edit) {
                val own = presets.presets.first()
                reduce { state.named(own) }
            }
        }
    }

    /** The title of a preset comes from the presets, as they are read. */
    private fun tabFor(route: PresetEditRoute): PresetTab =
        route.presetId?.let { PresetTab.Edit(it) } ?: PresetTab.New(newPresetTab(++made))

    /** The tabs of the presets titled by their names in [own]; a preset not there keeps the title it had. */
    private fun PresetsContainerState.named(own: List<EntryPreset>): PresetsContainerState {
        val names = own.mapNotNull { preset -> preset.id?.let { it.value to preset.name.orEmpty() } }.toMap()
        return copy(tabs = tabs.map { if (it is PresetTab.Edit) it.copy(title = names[it.presetId] ?: it.title) else it })
    }

    /** The tab [key] gone; shown instead - the next one, else the one before. */
    private fun PresetsContainerState.closed(key: String): PresetsContainerState =
        closeTab(tabs, active, key, PresetTab::key)?.let { copy(tabs = it.tabs, active = it.active) } ?: this
}
