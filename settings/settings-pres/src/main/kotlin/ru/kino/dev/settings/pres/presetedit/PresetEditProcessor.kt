package ru.kino.dev.settings.pres.presetedit

import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ru.kino.dev.core.preset.CustomPreset
import ru.kino.dev.core.preset.FieldNameProblem
import ru.kino.dev.core.preset.FieldPosition
import ru.kino.dev.core.preset.PresetEditor
import ru.kino.dev.core.preset.PresetFailure
import ru.kino.dev.core.preset.PresetId
import ru.kino.dev.core.preset.PresetNameProblem
import ru.kino.dev.core.preset.PresetProblem
import ru.kino.dev.core.preset.PresetRepository
import ru.kino.dev.core.preset.PresetValidator
import ru.kino.dev.log.ErrorLog
import ru.kino.dev.mvi.textIntent
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.Navigator

/**
 * @param name the name of the preset, as typed
 * @param cards the names of the fields of each card, as [ru.kino.dev.core.preset.CustomPreset.of] takes them:
 * the first - what goes after the user name and the password; the rest - the cards the person added
 * @param problems what the last save was refused for, until that part is edited
 * @param saving the preset is being made - the fab waits, the form takes nothing
 * @param editing a preset that is there is edited, not a new one made
 * @param loading the preset to edit is read - the fab waits, the form takes nothing
 * @param initialName the name as read; empty for a new preset
 * @param initialCards the cards as read; the first card alone for a new preset
 * @param message for the snackbar, until it takes it
 */
data class PresetEditState(
    val name: String = "",
    val cards: List<List<String>> = EMPTY_CARDS,
    val problems: List<PresetProblem> = emptyList(),
    val saving: Boolean = false,
    val editing: Boolean = false,
    val loading: Boolean = false,
    val initialName: String = "",
    val initialCards: List<List<String>> = EMPTY_CARDS,
    val dialog: PresetEditDialog? = null,
    val message: PresetEditMessage? = null,
) {
    /** Anything typed, added or removed since it was read: "back" asks first. */
    val changed: Boolean get() = name != initialName || cards != initialCards

    /**
     * A name of a field read is gone - renamed, deleted, gone with its card: entries with the preset will show
     * their values of it as their own fields (pass-docs, Step 62).
     */
    val dropsFields: Boolean get() = (initialCards.flatten() - cards.flatten().toSet()).isNotEmpty()

    /** One more field can go into the card [card] - it has room (pass-docs, Step 40, fix 19.1). */
    fun canAddField(card: Int): Boolean = PresetValidator.canAddField(cards, card)

    /** One more card can be added - the preset has room (pass-docs, Step 40, fix 19.2). */
    val canAddCard: Boolean get() = PresetValidator.canAddCard(cards)

    /** Why the name was refused, if it was. */
    val nameProblem: PresetNameProblem? get() = problems.firstNotNullOfOrNull { (it as? PresetProblem.Name)?.problem }

    /** Why the name of the field [at] was refused, if it was. */
    fun fieldProblem(at: FieldPosition): FieldNameProblem? =
        problems.firstNotNullOfOrNull { (it as? PresetProblem.FieldName)?.takeIf { bad -> bad.at == at }?.problem }

    private companion object {
        // The first card is always there: the user name and the password are in it
        val EMPTY_CARDS = listOf(emptyList<String>())
    }
}

/**
 * Creating a preset of one's own, or editing the one of [presetId] (pass-docs, vault-our.md "Экран создания
 * пресетов", Step 62): its name, the fields of its first card - after the user name and the password - and of the
 * cards the person adds; the notes come last by themselves.
 *
 * The checks are [PresetValidator]'s, the same the save is refused by. A preset made, the screen goes back -
 * the form of the entry picks the new preset up when it is shown again. An edit keeps the id, so the entries keep
 * the preset; one that drops names of its fields asks first - the entries keep their values under those names.
 */
@HiltViewModel(assistedFactory = PresetEditProcessor.Factory::class)
class PresetEditProcessor @AssistedInject constructor(
    @Assisted private val presetId: String?,
    private val presets: PresetRepository,
    private val editor: PresetEditor,
    private val navigator: Navigator,
    private val log: ErrorLog,
) : ViewModel(), OrbitContainerHost<PresetEditState, PresetEditState, Nothing> {

    @AssistedFactory
    interface Factory {
        fun create(presetId: String?): PresetEditProcessor
    }

    override val container = orbitContainer<PresetEditState, Nothing>(
        PresetEditState(editing = presetId != null, loading = presetId != null),
    )

    fun action(action: PresetEditAction): Job = when (action) {
        // Typed text: the state is updated before the field draws again, or the cursor jumps
        is PresetEditAction.NameChanged -> textIntent {
            reduce {
                if (state.saving) state else state.copy(name = action.name, problems = state.problems.filterNot { it is PresetProblem.Name })
            }
        }

        is PresetEditAction.DialogNameChanged -> textIntent {
            reduce { state.copy(dialog = state.dialog?.typed(action.name)) }
        }

        else -> intent { handle(action) }
    }

    private suspend fun Syntax<PresetEditState, Nothing>.handle(action: PresetEditAction) {
        when (action) {
            is PresetEditAction.NameChanged, is PresetEditAction.DialogNameChanged -> Unit // in action()

            PresetEditAction.Load -> load()

            is PresetEditAction.AddField -> if (!state.saving && state.canAddField(action.card)) {
                reduce { state.copy(dialog = PresetEditDialog.AddField(action.card)) }
            }

            is PresetEditAction.RenameField -> state.fieldName(action.at)?.let { name ->
                if (!state.saving) reduce { state.copy(dialog = PresetEditDialog.RenameField(action.at, name)) }
            }

            is PresetEditAction.DeleteField -> state.fieldName(action.at)?.let { name ->
                if (!state.saving) reduce { state.copy(dialog = PresetEditDialog.DeleteField(action.at, name)) }
            }

            PresetEditAction.AddCard -> if (!state.saving && state.canAddCard) {
                reduce { state.copy(cards = state.cards + listOf(emptyList())) }
            }

            // The first card is not the person's to delete
            is PresetEditAction.DeleteCard -> if (!state.saving && action.card in 1..state.cards.lastIndex) {
                reduce { state.copy(dialog = PresetEditDialog.DeleteCard(action.card)) }
            }

            PresetEditAction.DialogConfirm -> confirm()

            PresetEditAction.DialogDismiss -> reduce { state.copy(dialog = null) }

            is PresetEditAction.Save -> save(action.standardName)

            PresetEditAction.Back -> when {
                state.saving -> Unit
                state.changed -> reduce { state.copy(dialog = PresetEditDialog.Leave) }
                else -> leave()
            }

            PresetEditAction.MessageShown -> reduce { state.copy(message = null) }
        }
    }

    private suspend fun Syntax<PresetEditState, Nothing>.confirm() {
        when (val dialog = state.dialog) {
            null -> Unit

            is PresetEditDialog.AddField -> {
                val problem = PresetValidator.fieldNameProblem(state.cards, dialog.name)
                if (problem != null) {
                    reduce { state.copy(dialog = dialog.copy(problem = problem)) }
                } else {
                    val name = PresetValidator.normalize(dialog.name)
                    reduce {
                        state.copy(
                            cards = state.cards.mapIndexed { i, keys -> if (i == dialog.card) keys + name else keys },
                            dialog = null,
                        )
                    }
                }
            }

            is PresetEditDialog.RenameField -> {
                val problem = PresetValidator.fieldNameProblem(state.cards, dialog.name, renamed = dialog.at)
                if (problem != null) {
                    reduce { state.copy(dialog = dialog.copy(problem = problem)) }
                } else {
                    val name = PresetValidator.normalize(dialog.name)
                    reduce {
                        state.copy(
                            cards = state.cards.change(dialog.at.card) { keys ->
                                keys.mapIndexed { i, key -> if (i == dialog.at.index) name else key }
                            },
                            dialog = null,
                            problems = state.problems.filterNot { it is PresetProblem.FieldName && it.at == dialog.at },
                        )
                    }
                }
            }

            // The fields after it move up: what was said about the names of fields no longer points right
            is PresetEditDialog.DeleteField -> reduce {
                state.copy(
                    cards = state.cards.change(dialog.at.card) { keys -> keys.filterIndexed { i, _ -> i != dialog.at.index } },
                    dialog = null,
                    problems = state.problems.filterNot { it is PresetProblem.FieldName },
                )
            }

            is PresetEditDialog.DeleteCard -> reduce {
                state.copy(
                    cards = state.cards.filterIndexed { i, _ -> i != dialog.card },
                    dialog = null,
                    problems = state.problems.filterNot { it is PresetProblem.FieldName },
                )
            }

            PresetEditDialog.Leave -> leave()

            is PresetEditDialog.FieldsDropped -> {
                reduce { state.copy(dialog = null) }
                write(dialog.standardName)
            }
        }
    }

    // The preset as it is kept; gone meanwhile - there is nothing to edit
    private suspend fun Syntax<PresetEditState, Nothing>.load() {
        val id = presetId ?: return
        if (!state.loading) return
        val preset = presets.presets.first().firstOrNull { it.id?.value == id }
        if (preset == null) {
            leave()
            return
        }
        val name = preset.name.orEmpty()
        val cards = CustomPreset.cards(preset)
        reduce {
            state.copy(name = name, cards = cards, initialName = name, initialCards = cards, loading = false)
        }
    }

    private suspend fun Syntax<PresetEditState, Nothing>.save(standardName: String) {
        if (state.saving || state.loading) return
        if (state.dropsFields) {
            reduce { state.copy(dialog = PresetEditDialog.FieldsDropped(standardName)) }
            return
        }
        write(standardName)
    }

    private suspend fun Syntax<PresetEditState, Nothing>.write(standardName: String) {
        reduce { state.copy(saving = true, problems = emptyList()) }
        try {
            val id = presetId
            if (id == null) {
                editor.create(state.name, state.cards, standardName)
            } else {
                editor.update(PresetId(id), state.name, state.cards, standardName)
            }
            leave()
        } catch (failure: PresetFailure.Invalid) {
            val tooMany = PresetProblem.TooMany in failure.problems
            reduce {
                state.copy(
                    saving = false,
                    problems = failure.problems - PresetProblem.TooMany,
                    message = if (tooMany) PresetEditMessage.TooMany else state.message,
                )
            }
        } catch (_: PresetFailure.NotFound) {
            reduce { state.copy(saving = false, message = PresetEditMessage.Gone) }
        } catch (failure: PresetFailure) {
            log.unexpected(TAG, failure)
            reduce { state.copy(saving = false, message = PresetEditMessage.NotSaved) }
        }
    }

    private suspend fun leave() {
        navigator.navigate(MviNavEvent.NavigateBack)
    }

    private companion object {
        const val TAG = "PresetEditProcessor"
    }
}

private fun PresetEditState.fieldName(at: FieldPosition): String? = cards.getOrNull(at.card)?.getOrNull(at.index)

private inline fun List<List<String>>.change(card: Int, change: (List<String>) -> List<String>) =
    mapIndexed { i, keys -> if (i == card) change(keys) else keys }

private fun PresetEditDialog.typed(name: String): PresetEditDialog = when (this) {
    is PresetEditDialog.AddField -> copy(name = name, problem = null)
    is PresetEditDialog.RenameField -> copy(name = name, problem = null)
    else -> this
}
