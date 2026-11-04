package ru.kino.dev.settings.pres.presetread

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.first
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ru.kino.dev.core.preset.EntryPreset
import ru.kino.dev.core.preset.PresetEditor
import ru.kino.dev.core.preset.PresetFailure
import ru.kino.dev.core.preset.PresetId
import ru.kino.dev.core.preset.PresetRepository
import ru.kino.dev.core.preset.StandardPreset
import ru.kino.dev.log.ErrorLog
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.Navigator
import ru.kino.dev.navigation.PresetEditRoute

/**
 * A preset read (pass-docs, Step 99, item 8), as an entry is: [presetId] - one of one's own, edited - the fab - and
 * deleted - its menu - from here; null - the standard one, only read.
 *
 * While the screen is shown it follows the presets kept: edited on its form - as saved; deleted beside it, in the list of
 * a container - back.
 */
class PresetReadProcessor(
    private val presetId: String?,
    private val presets: PresetRepository,
    private val editor: PresetEditor,
    private val navigator: Navigator,
    private val log: ErrorLog,
) : ViewModel(), OrbitContainerHost<PresetReadState, PresetReadState, Nothing>, PresetReadActions {

    /** It is leaving - deleted here: the presets without it are no reason to go back again. */
    private var leaving = false

    override val container = orbitContainer<PresetReadState, Nothing>(
        PresetReadState(preset = if (presetId == null) StandardPreset.preset else null),
    ) {
        // While the screen is shown: the preset as kept, changed or deleted beside it too
        if (presetId != null) repeatOnSubscription { presets.presets.collect { show(it) } }
    }

    override fun onLoad() {
        intent { if (presetId != null) show(presets.presets.first()) }
    }

    override fun onBack() {
        intent { leave() }
    }

    override fun onEdit() {
        intent {
            if (state.own && !state.working) navigator.navigate(MviNavEvent.NavigateTo(PresetEditRoute(presetId)))
        }
    }

    override fun onDelete() {
        intent { if (state.own && !state.working) reduce { state.copy(deleting = true) } }
    }

    override fun onDeleteConfirm() {
        intent { delete() }
    }

    override fun onDeleteDismiss() {
        intent { reduce { state.copy(deleting = false) } }
    }

    override fun onMessageShown() {
        intent { reduce { state.copy(message = null) } }
    }

    // One's own as kept; gone - deleted elsewhere - there is nothing to read
    private suspend fun Syntax<PresetReadState, Nothing>.show(own: List<EntryPreset>) {
        if (leaving) return
        val preset = own.firstOrNull { it.id?.value == presetId }
        if (preset == null) leave() else reduce { state.copy(preset = preset) }
    }

    private suspend fun Syntax<PresetReadState, Nothing>.delete() {
        val id = presetId ?: return
        if (!state.deleting) return
        reduce { state.copy(deleting = false, working = true) }
        leaving = true
        try {
            editor.delete(PresetId(id))
        } catch (failure: PresetFailure) {
            leaving = false
            log.unexpected(TAG, failure)
            reduce { state.copy(working = false, message = PresetReadMessage.NotDeleted) }
            return
        }
        navigator.navigate(MviNavEvent.NavigateBack)
    }

    private suspend fun leave() {
        leaving = true
        navigator.navigate(MviNavEvent.NavigateBack)
    }

    private companion object {
        const val TAG = "PresetReadProcessor"
    }
}
