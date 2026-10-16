package ru.kino.dev.vault.pres.presetcreate

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ru.kino.dev.core.preset.FieldNameProblem
import ru.kino.dev.core.preset.FieldPosition
import ru.kino.dev.core.preset.PresetEditor
import ru.kino.dev.core.preset.PresetFailure
import ru.kino.dev.core.preset.PresetNameProblem
import ru.kino.dev.core.preset.PresetProblem
import ru.kino.dev.core.preset.PresetValidator
import ru.kino.dev.log.ErrorLog
import ru.kino.dev.mvi.textIntent
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.Navigator
import javax.inject.Inject

/**
 * @param name the name of the preset, as typed
 * @param cards the names of the fields of each card, as [ru.kino.dev.core.preset.CustomPreset.of] takes them:
 * the first - what goes after the user name and the password; the rest - the cards the person added
 * @param problems what the last save was refused for, until that part is edited
 * @param saving the preset is being made - the fab waits, the form takes nothing
 * @param message for the snackbar, until it takes it
 */
data class PresetCreateState(
    val name: String = "",
    val cards: List<List<String>> = EMPTY_CARDS,
    val problems: List<PresetProblem> = emptyList(),
    val saving: Boolean = false,
    val dialog: PresetCreateDialog? = null,
    val message: PresetCreateMessage? = null,
) {
    /** Anything typed or added: "back" asks first. */
    val changed: Boolean get() = name.isNotEmpty() || cards != EMPTY_CARDS

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
 * Creating a preset of one's own (pass-docs, vault-our.md "Экран создания пресетов"): its name, the fields of
 * its first card - after the user name and the password - and of the cards the person adds; the notes come
 * last by themselves.
 *
 * The checks are [PresetValidator]'s, the same the save is refused by. A preset made, the screen goes back -
 * the form of the entry picks the new preset up when it is shown again.
 */
@HiltViewModel
class PresetCreateProcessor @Inject constructor(
    private val editor: PresetEditor,
    private val navigator: Navigator,
    private val log: ErrorLog,
) : ViewModel(), OrbitContainerHost<PresetCreateState, PresetCreateState, Nothing> {

    override val container = orbitContainer<PresetCreateState, Nothing>(PresetCreateState())

    fun action(action: PresetCreateAction): Job = when (action) {
        // Typed text: the state is updated before the field draws again, or the cursor jumps
        is PresetCreateAction.NameChanged -> textIntent {
            reduce {
                if (state.saving) state else state.copy(name = action.name, problems = state.problems.filterNot { it is PresetProblem.Name })
            }
        }

        is PresetCreateAction.DialogNameChanged -> textIntent {
            reduce { state.copy(dialog = state.dialog?.typed(action.name)) }
        }

        else -> intent { handle(action) }
    }

    private suspend fun Syntax<PresetCreateState, Nothing>.handle(action: PresetCreateAction) {
        when (action) {
            is PresetCreateAction.NameChanged, is PresetCreateAction.DialogNameChanged -> Unit // in action()

            is PresetCreateAction.AddField -> if (!state.saving && state.canAddField(action.card)) {
                reduce { state.copy(dialog = PresetCreateDialog.AddField(action.card)) }
            }

            is PresetCreateAction.RenameField -> state.fieldName(action.at)?.let { name ->
                if (!state.saving) reduce { state.copy(dialog = PresetCreateDialog.RenameField(action.at, name)) }
            }

            is PresetCreateAction.DeleteField -> state.fieldName(action.at)?.let { name ->
                if (!state.saving) reduce { state.copy(dialog = PresetCreateDialog.DeleteField(action.at, name)) }
            }

            PresetCreateAction.AddCard -> if (!state.saving && state.canAddCard) {
                reduce { state.copy(cards = state.cards + listOf(emptyList())) }
            }

            // The first card is not the person's to delete
            is PresetCreateAction.DeleteCard -> if (!state.saving && action.card in 1..state.cards.lastIndex) {
                reduce { state.copy(dialog = PresetCreateDialog.DeleteCard(action.card)) }
            }

            PresetCreateAction.DialogConfirm -> confirm()

            PresetCreateAction.DialogDismiss -> reduce { state.copy(dialog = null) }

            is PresetCreateAction.Save -> save(action.standardName)

            PresetCreateAction.Back -> when {
                state.saving -> Unit
                state.changed -> reduce { state.copy(dialog = PresetCreateDialog.Leave) }
                else -> leave()
            }

            PresetCreateAction.MessageShown -> reduce { state.copy(message = null) }
        }
    }

    private suspend fun Syntax<PresetCreateState, Nothing>.confirm() {
        when (val dialog = state.dialog) {
            null -> Unit

            is PresetCreateDialog.AddField -> {
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

            is PresetCreateDialog.RenameField -> {
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
            is PresetCreateDialog.DeleteField -> reduce {
                state.copy(
                    cards = state.cards.change(dialog.at.card) { keys -> keys.filterIndexed { i, _ -> i != dialog.at.index } },
                    dialog = null,
                    problems = state.problems.filterNot { it is PresetProblem.FieldName },
                )
            }

            is PresetCreateDialog.DeleteCard -> reduce {
                state.copy(
                    cards = state.cards.filterIndexed { i, _ -> i != dialog.card },
                    dialog = null,
                    problems = state.problems.filterNot { it is PresetProblem.FieldName },
                )
            }

            PresetCreateDialog.Leave -> leave()
        }
    }

    private suspend fun Syntax<PresetCreateState, Nothing>.save(standardName: String) {
        if (state.saving) return
        reduce { state.copy(saving = true, problems = emptyList()) }
        try {
            editor.create(state.name, state.cards, standardName)
            leave()
        } catch (failure: PresetFailure.Invalid) {
            val tooMany = PresetProblem.TooMany in failure.problems
            reduce {
                state.copy(
                    saving = false,
                    problems = failure.problems - PresetProblem.TooMany,
                    message = if (tooMany) PresetCreateMessage.TooMany else state.message,
                )
            }
        } catch (failure: PresetFailure) {
            log.unexpected(TAG, failure)
            reduce { state.copy(saving = false, message = PresetCreateMessage.NotSaved) }
        }
    }

    private suspend fun leave() {
        navigator.navigate(MviNavEvent.NavigateBack)
    }

    private companion object {
        const val TAG = "PresetCreateProcessor"
    }
}

private fun PresetCreateState.fieldName(at: FieldPosition): String? = cards.getOrNull(at.card)?.getOrNull(at.index)

private inline fun List<List<String>>.change(card: Int, change: (List<String>) -> List<String>) =
    mapIndexed { i, keys -> if (i == card) change(keys) else keys }

private fun PresetCreateDialog.typed(name: String): PresetCreateDialog = when (this) {
    is PresetCreateDialog.AddField -> copy(name = name, problem = null)
    is PresetCreateDialog.RenameField -> copy(name = name, problem = null)
    else -> this
}
