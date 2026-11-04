package ru.kino.dev.settings.pres.presetedit

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.first
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ru.kino.dev.core.FieldType
import ru.kino.dev.core.preset.CustomPreset
import ru.kino.dev.core.preset.FieldPosition
import ru.kino.dev.core.preset.PresetEditor
import ru.kino.dev.core.preset.PresetFailure
import ru.kino.dev.core.preset.PresetId
import ru.kino.dev.core.preset.PresetProblem
import ru.kino.dev.core.preset.PresetRepository
import ru.kino.dev.core.preset.PresetValidator
import ru.kino.dev.log.ErrorLog
import ru.kino.dev.mvi.textIntent
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.Navigator
import ru.kino.dev.navigation.PresetEditRoute
import ru.kino.dev.settings.pres.navigation.PresetReadRoute

/**
 * Creating a preset of one's own, or editing the one of [presetId] (pass-docs, vault-our.md "Экран создания
 * пресетов", Step 62): its name, the fields of its first card - after the user name and the password - and of the
 * cards the person adds; the notes come last by themselves.
 *
 * The checks are [PresetValidator]'s, the same the save is refused by. A preset made, the screen goes back -
 * the form of the entry picks the new preset up when it is shown again; made from the list of the presets -
 * [readSaved] - its reading in place of the form (pass-docs, Step 99, item 8). A preset that is there is deleted from
 * its menu too, past its reading. An edit keeps the id, so the entries keep
 * the preset; one that drops names of its fields asks first - the entries keep their values under those names.
 */
class PresetEditProcessor(
    private val presetId: String?,
    private val readSaved: Boolean,
    private val presets: PresetRepository,
    private val editor: PresetEditor,
    private val navigator: Navigator,
    private val log: ErrorLog,
) : ViewModel(), OrbitContainerHost<PresetEditState, PresetEditState, Nothing>, PresetEditActions {

    override val container = orbitContainer<PresetEditState, Nothing>(
        PresetEditState(editing = presetId != null, loading = presetId != null),
    )

    override fun onNameChange(name: String) {
        // Typed text: the state is updated before the field draws again, or the cursor jumps
        textIntent {
            reduce {
                if (state.saving) state else state.copy(name = name, problems = state.problems.filterNot { it is PresetProblem.Name })
            }
        }
    }

    override fun onDialogNameChange(name: String) {
        textIntent {
            reduce { state.copy(dialog = state.dialog?.typed(name)) }
        }
    }

    override fun onLoad() {
        intent { load() }
    }

    override fun onAddField(card: Int) {
        intent {
            if (!state.saving && state.canAddField(card)) {
                reduce { state.copy(dialog = PresetEditDialog.AddField(card)) }
            }
        }
    }

    override fun onEditField(at: FieldPosition) {
        intent {
            state.fieldName(at)?.let { name ->
                if (!state.saving) {
                    reduce {
                        state.copy(
                            dialog = PresetEditDialog.EditField(at, name, name in state.protectedKeys, state.fieldTypes[name]),
                        )
                    }
                }
            }
        }
    }

    override fun onDialogProtectedChange(protected: Boolean) {
        intent {
            reduce {
                state.copy(dialog = state.dialog?.switched(protected))
            }
        }
    }

    override fun onDialogTypeChange(type: FieldType) {
        intent {
            reduce {
                state.copy(dialog = state.dialog?.typedAs(type))
            }
        }
    }

    override fun onDeleteField(at: FieldPosition) {
        intent {
            state.fieldName(at)?.let { name ->
                if (!state.saving) reduce { state.copy(dialog = PresetEditDialog.DeleteField(at, name)) }
            }
        }
    }

    override fun onAddCard() {
        intent {
            if (!state.saving && state.canAddCard) {
                reduce { state.copy(cards = state.cards + listOf(emptyList())) }
            }
        }
    }

    override fun onDeleteCard(card: Int) {
        // The first card is not the person's to delete
        intent {
            if (!state.saving && card in 1..state.cards.lastIndex) {
                reduce { state.copy(dialog = PresetEditDialog.DeleteCard(card)) }
            }
        }
    }

    override fun onDialogConfirm() {
        intent { confirm() }
    }

    override fun onDialogDismiss() {
        intent { reduce { state.copy(dialog = null) } }
    }

    override fun onSave(standardName: String) {
        intent { save(standardName) }
    }

    override fun onBack() {
        intent {
            when {
                state.saving -> Unit
                state.changed -> reduce { state.copy(dialog = PresetEditDialog.Leave()) }
                else -> leave()
            }
        }
    }

    override fun onClose() {
        intent {
            when {
                state.saving -> Unit
                state.changed -> reduce { state.copy(dialog = PresetEditDialog.Leave(closing = true)) }
                else -> close()
            }
        }
    }

    override fun onDelete() {
        intent {
            if (state.editing && !state.loading && !state.saving) {
                reduce { state.copy(dialog = PresetEditDialog.Delete(state.initialName)) }
            }
        }
    }

    override fun onMessageShown() {
        intent { reduce { state.copy(message = null) } }
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
                            protectedKeys = state.protectedKeys.with(name, dialog.protected),
                            fieldTypes = state.fieldTypes.with(name, dialog.type),
                            dialog = null,
                        )
                    }
                }
            }

            is PresetEditDialog.EditField -> {
                val problem = PresetValidator.fieldNameProblem(state.cards, dialog.name, renamed = dialog.at)
                val old = state.fieldName(dialog.at)
                if (problem != null) {
                    reduce { state.copy(dialog = dialog.copy(problem = problem)) }
                } else if (old != null) {
                    val name = PresetValidator.normalize(dialog.name)
                    reduce {
                        state.copy(
                            cards = state.cards.change(dialog.at.card) { keys ->
                                keys.mapIndexed { i, key -> if (i == dialog.at.index) name else key }
                            },
                            // The protection and the type go with the field, under its new name
                            protectedKeys = (state.protectedKeys - old).with(name, dialog.protected),
                            fieldTypes = (state.fieldTypes - old).with(name, dialog.type),
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
                    protectedKeys = state.protectedKeys - dialog.name,
                    fieldTypes = state.fieldTypes - dialog.name,
                    dialog = null,
                    problems = state.problems.filterNot { it is PresetProblem.FieldName },
                )
            }

            is PresetEditDialog.DeleteCard -> reduce {
                state.copy(
                    cards = state.cards.filterIndexed { i, _ -> i != dialog.card },
                    protectedKeys = state.protectedKeys - state.cards[dialog.card].toSet(),
                    fieldTypes = state.fieldTypes - state.cards[dialog.card].toSet(),
                    dialog = null,
                    problems = state.problems.filterNot { it is PresetProblem.FieldName },
                )
            }

            is PresetEditDialog.Leave -> if (dialog.closing) close() else leave()

            is PresetEditDialog.Delete -> {
                reduce { state.copy(dialog = null) }
                delete()
            }

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
        val protectedKeys = CustomPreset.protectedKeys(preset)
        val fieldTypes = CustomPreset.fieldTypes(preset)
        reduce {
            state.copy(
                name = name,
                cards = cards,
                protectedKeys = protectedKeys,
                fieldTypes = fieldTypes,
                initialName = name,
                initialCards = cards,
                initialProtectedKeys = protectedKeys,
                initialFieldTypes = fieldTypes,
                loading = false,
            )
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
                val made = editor.create(state.name, state.cards, standardName, state.protectedKeys, state.fieldTypes)
                val madeId = made.id?.value
                if (readSaved && madeId != null) openMade(madeId) else leave()
            } else {
                editor.update(PresetId(id), state.name, state.cards, standardName, state.protectedKeys, state.fieldTypes)
                leave()
            }
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

    private suspend fun Syntax<PresetEditState, Nothing>.delete() {
        val id = presetId ?: return
        reduce { state.copy(saving = true) }
        try {
            editor.delete(PresetId(id))
        } catch (failure: PresetFailure) {
            log.unexpected(TAG, failure)
            reduce { state.copy(saving = false, message = PresetEditMessage.NotDeleted) }
            return
        }
        leavePastReading(id)
    }

    private suspend fun leave() {
        navigator.navigate(MviNavEvent.NavigateBack)
    }

    /** The form closed - the cross of its tab: a preset that is there - past its reading; a new one - back. */
    private suspend fun close() {
        if (presetId == null) leave() else leavePastReading(presetId)
    }

    /**
     * Back to where its reading was opened from - the list - past the reading of the preset [id] too, when the form was
     * opened from it: the preset deleted, the form closed.
     */
    private suspend fun leavePastReading(id: String) {
        navigator.navigate(MviNavEvent.NavigateBackTo(PresetReadRoute(id), inclusive = true, orBack = true))
    }

    /** The preset [id] made from the list: its reading in place of the form - "back" from it goes to the list. */
    private suspend fun openMade(id: String) {
        val form = PresetEditRoute(readSaved = true)
        navigator.navigate(MviNavEvent.NavigateTo(PresetReadRoute(id), popUpTo = form, inclusive = true))
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
    is PresetEditDialog.EditField -> copy(name = name, problem = null)
    else -> this
}

private fun PresetEditDialog.switched(protected: Boolean): PresetEditDialog = when (this) {
    is PresetEditDialog.AddField -> copy(protected = protected)
    is PresetEditDialog.EditField -> copy(protected = protected)
    else -> this
}

private fun Set<String>.with(name: String, protected: Boolean): Set<String> = if (protected) this + name else this - name

// The kind picked again keeps what the type carries - the country of a phone (pass-docs, Step 77)
private fun PresetEditDialog.typedAs(type: FieldType): PresetEditDialog = when (this) {
    is PresetEditDialog.AddField -> copy(type = this.type.retyped(type))
    is PresetEditDialog.EditField -> copy(type = this.type.retyped(type))
    else -> this
}

// By the key itself: a type this app does not know, shown as text, becomes text when text is picked
private fun String?.retyped(type: FieldType): String? = if (this?.substringBefore(':') == type.key) this else type.key

// Text is no type: the field is left out
private fun Map<String, String>.with(name: String, type: String?): Map<String, String> =
    if (type == null) this - name else this + (name to type)
