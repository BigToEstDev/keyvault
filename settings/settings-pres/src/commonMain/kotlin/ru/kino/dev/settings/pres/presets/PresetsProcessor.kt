package ru.kino.dev.settings.pres.presets

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ru.kino.dev.core.preset.EntryPreset
import ru.kino.dev.core.preset.PresetEditor
import ru.kino.dev.core.preset.PresetFailure
import ru.kino.dev.core.preset.PresetRepository
import ru.kino.dev.core.preset.PresetValidator
import ru.kino.dev.log.ErrorLog
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.Navigator
import ru.kino.dev.navigation.PresetEditRoute
import ru.kino.dev.settings.pres.common.keepSetting
import ru.kino.dev.settings.presets.PresetSettingsRepository

/**
 * @param usePresets whether presets are used; null until read - its choice is disabled
 * @param presets the presets of one's own, in the order they were made; null until read
 * @param deleting "Delete the preset?" is open for it
 * @param message for the snackbar, until it takes it
 */
data class PresetsState(
    val usePresets: Boolean? = null,
    val presets: List<EntryPreset>? = null,
    val deleting: EntryPreset? = null,
    val message: PresetsMessage? = null,
) {
    /** One more preset can be made - the most a person has is not reached (pass-docs, Step 44). */
    val canCreate: Boolean get() = presets?.let { PresetValidator.canAdd(it) } ?: false
}

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
) : ViewModel(), OrbitContainerHost<PresetsState, PresetsState, Nothing> {

    override val container = orbitContainer<PresetsState, Nothing>(PresetsState())

    fun action(action: PresetsAction): Job = intent {
        when (action) {
            PresetsAction.Load -> {
                val usePresets = settings.usePresets.first()
                val own = presets.presets.first()
                reduce { state.copy(usePresets = usePresets, presets = own) }
            }

            PresetsAction.Back -> navigator.navigate(MviNavEvent.NavigateBack)

            is PresetsAction.SetUsePresets -> keepSetting(
                chosen = action.on,
                current = state.usePresets,
                kept = settings.usePresets,
                show = { copy(usePresets = it) },
                notSaved = { copy(message = PresetsMessage.NotSaved) },
            ) { settings.saveUsePresets(it) }

            PresetsAction.Create -> if (state.canCreate) navigator.navigate(MviNavEvent.NavigateTo(PresetEditRoute()))

            is PresetsAction.Edit -> navigator.navigate(MviNavEvent.NavigateTo(PresetEditRoute(action.id.value)))

            // The standard preset is not one's own to delete
            is PresetsAction.Delete -> if (action.preset.id != null) reduce { state.copy(deleting = action.preset) }

            PresetsAction.DeleteConfirm -> delete()

            PresetsAction.DeleteDismiss -> reduce { state.copy(deleting = null) }

            PresetsAction.MessageShown -> reduce { state.copy(message = null) }
        }
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
