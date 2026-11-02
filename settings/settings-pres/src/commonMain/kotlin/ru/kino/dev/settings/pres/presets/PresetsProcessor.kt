package ru.kino.dev.settings.pres.presets

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
import ru.kino.dev.log.ErrorLog
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.Navigator
import ru.kino.dev.navigation.PresetEditRoute
import ru.kino.dev.settings.pres.common.keepSetting
import ru.kino.dev.settings.presets.PresetSettingsRepository

/**
 * The presets (pass-docs, Step 62): whether the screens of an entry offer them, the standard one and those of one's
 * own - edited and deleted here, made here or from an entry. They are one list for every database, kept by the app.
 *
 * Read each time the screen is shown: back from a preset made or edited, the list is as kept. A preset deleted -
 * its entries open with the standard preset, its fields as their own (pass-docs, `vault-our.md`).
 */
class PresetsProcessor(
    private val settings: PresetSettingsRepository,
    private val presets: PresetRepository,
    private val editor: PresetEditor,
    private val navigator: Navigator,
    private val log: ErrorLog,
) : ViewModel(), OrbitContainerHost<PresetsState, PresetsState, Nothing>, PresetsActions {

    override val container = orbitContainer<PresetsState, Nothing>(PresetsState())

    override fun onLoad() {
        intent {
            val usePresets = settings.usePresets.first()
            val own = presets.presets.first()
            reduce { state.copy(usePresets = usePresets, presets = own) }
        }
    }

    override fun onBack() {
        intent { navigator.navigate(MviNavEvent.NavigateBack) }
    }

    override fun onSetUsePresets(on: Boolean) {
        intent {
            keepSetting(
                chosen = on,
                current = state.usePresets,
                kept = settings.usePresets,
                show = { copy(usePresets = it) },
                notSaved = { copy(message = PresetsMessage.NotSaved) },
            ) { settings.saveUsePresets(it) }
        }
    }

    override fun onCreate() {
        intent { if (state.canCreate) navigator.navigate(MviNavEvent.NavigateTo(PresetEditRoute())) }
    }

    override fun onEdit(id: PresetId) {
        intent { navigator.navigate(MviNavEvent.NavigateTo(PresetEditRoute(id.value))) }
    }

    override fun onDelete(preset: EntryPreset) {
        // The standard preset is not one's own to delete
        intent { if (preset.id != null) reduce { state.copy(deleting = preset) } }
    }

    override fun onDeleteConfirm() {
        intent { delete() }
    }

    override fun onDeleteDismiss() {
        intent { reduce { state.copy(deleting = null) } }
    }

    override fun onMessageShown() {
        intent { reduce { state.copy(message = null) } }
    }

    private suspend fun Syntax<PresetsState, Nothing>.delete() {
        val id = state.deleting?.id ?: return
        reduce { state.copy(deleting = null) }
        try {
            editor.delete(id)
            val own = presets.presets.first()
            reduce { state.copy(presets = own) }
        } catch (failure: PresetFailure) {
            log.unexpected(TAG, failure)
            reduce { state.copy(message = PresetsMessage.NotDeleted) }
        }
    }

    private companion object {
        const val TAG = "PresetsProcessor"
    }
}
