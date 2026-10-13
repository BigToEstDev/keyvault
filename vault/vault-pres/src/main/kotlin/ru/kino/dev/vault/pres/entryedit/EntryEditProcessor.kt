package ru.kino.dev.vault.pres.entryedit

import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ru.kino.dev.log.ErrorLog
import ru.kino.dev.mvi.textIntent
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.Navigator
import ru.kino.dev.vault.entry.EntryContent
import ru.kino.dev.vault.entry.EntryEditor
import ru.kino.dev.vault.entry.EntryField
import ru.kino.dev.vault.entry.EntryFailure
import ru.kino.dev.vault.entry.EntryRepository
import ru.kino.dev.vault.entry.EntryValidator
import ru.kino.dev.vault.entry.FieldProblem
import ru.kino.dev.vault.entry.NameProblem
import ru.kino.dev.vault.entry.StandardPreset
import ru.kino.dev.vault.group.EntryId
import ru.kino.dev.vault.group.GroupId

/**
 * @param creating a new entry, not one of the database
 * @param loading the entry is being read - the skeleton is shown; never for a new one
 * @param content what the form holds now; null until the entry is read
 * @param initial what the form opened with - the blank or the entry as read: "back" asks before leaving
 * anything else
 * @param shownOwn indexes of protected own fields shown in clear
 * @param problems what the last save was refused for, until the field is edited
 * @param saving the change is being made and written - the fab waits, the fields take nothing
 * @param message for the snackbar, until it takes it
 */
data class EntryEditState(
    val creating: Boolean,
    val loading: Boolean = false,
    val content: EntryContent? = null,
    val initial: EntryContent? = null,
    val passwordVisible: Boolean = false,
    val shownOwn: Set<Int> = emptySet(),
    val problems: List<FieldProblem> = emptyList(),
    val saving: Boolean = false,
    val dialog: EntryEditDialog? = null,
    val message: EntryEditMessage? = null,
) {
    /** The form differs from what it opened with. */
    val changed: Boolean get() = content != initial

    /** The required field of the preset under [key] was refused for being empty. */
    fun isMissing(key: String): Boolean = problems.any { it is FieldProblem.Required && it.key == key }

    /** Why the name of the own field at [index] was refused, if it was. */
    fun nameProblem(index: Int): NameProblem? =
        problems.firstNotNullOfOrNull { (it as? FieldProblem.BadName)?.takeIf { bad -> bad.index == index }?.problem }
}

/**
 * Creating an entry, or editing one - one form, two ways in (pass-docs, vault-our.md "Экран редактирования
 * записи", decisions.md "Экраны записи").
 *
 * The checks are [EntryValidator]'s, the same the save is refused by: the form does not check twice, it
 * shows what [EntryEditor] refuses. A save that goes through, or a database that cannot take it any more -
 * the entry or its group gone - closes the screen; the screen it goes back to reads anew.
 *
 * The entry is read once, when the screen opens: what the person types must not be lost when the screen is
 * shown again.
 *
 * @param groupId where a new entry goes; null when editing
 * @param entryId the entry edited; null when creating
 */
@HiltViewModel(assistedFactory = EntryEditProcessor.Factory::class)
class EntryEditProcessor @AssistedInject constructor(
    @Assisted("dbKey") private val dbKey: String,
    @Assisted("groupId") private val groupId: String?,
    @Assisted("entryId") private val entryId: String?,
    private val entries: EntryRepository,
    private val editor: EntryEditor,
    private val navigator: Navigator,
    private val log: ErrorLog,
) : ViewModel(), OrbitContainerHost<EntryEditState, EntryEditState, Nothing> {

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("dbKey") dbKey: String,
            @Assisted("groupId") groupId: String?,
            @Assisted("entryId") entryId: String?,
        ): EntryEditProcessor
    }

    init {
        require((groupId == null) != (entryId == null)) { "either a group to create in or an entry to edit" }
    }

    override val container = orbitContainer<EntryEditState, Nothing>(
        if (entryId == null) {
            val blank = StandardPreset.preset.blank()
            EntryEditState(creating = true, content = blank, initial = blank)
        } else {
            EntryEditState(creating = false, loading = true)
        },
    ) {
        if (entryId != null) load(EntryId(entryId))
    }

    fun action(action: EntryEditAction): Job = when (action) {
        // Typed text: the state is updated before the field draws again, or the cursor jumps
        is EntryEditAction.ValueChanged -> textIntent {
            reduce {
                state.edited { withValue(action.key, action.value) }
                    .copy(problems = state.problems.filterNot { it is FieldProblem.Required && it.key == action.key })
            }
        }

        is EntryEditAction.OwnValueChanged -> textIntent {
            reduce {
                state.edited {
                    copy(ownFields = ownFields.mapIndexed { i, field -> if (i == action.index) field.copy(value = action.value) else field })
                }
            }
        }

        is EntryEditAction.DialogNameChanged -> textIntent {
            reduce { state.copy(dialog = state.dialog?.typed(action.name)) }
        }

        else -> intent { handle(action) }
    }

    private suspend fun Syntax<EntryEditState, Nothing>.handle(action: EntryEditAction) {
        when (action) {
            is EntryEditAction.ValueChanged,
            is EntryEditAction.OwnValueChanged,
            is EntryEditAction.DialogNameChanged,
            -> Unit // in action()

            EntryEditAction.TogglePassword -> reduce { state.copy(passwordVisible = !state.passwordVisible) }

            is EntryEditAction.ToggleOwn -> reduce {
                val shown = state.shownOwn
                state.copy(shownOwn = if (action.index in shown) shown - action.index else shown + action.index)
            }

            EntryEditAction.ChoosePreset -> if (state.editable) {
                reduce { state.copy(dialog = EntryEditDialog.Preset) }
            }

            EntryEditAction.AddField -> if (state.editable) {
                reduce { state.copy(dialog = EntryEditDialog.AddField()) }
            }

            is EntryEditAction.RenameField -> state.ownField(action.index)?.let { field ->
                if (state.editable) reduce { state.copy(dialog = EntryEditDialog.RenameField(action.index, field.key)) }
            }

            is EntryEditAction.DeleteField -> state.ownField(action.index)?.let { field ->
                if (state.editable) reduce { state.copy(dialog = EntryEditDialog.DeleteField(action.index, field.key)) }
            }

            EntryEditAction.DialogConfirm -> confirm()

            EntryEditAction.DialogDismiss -> when (state.dialog) {
                // Nothing to stay for: the change is made, the screen goes as after a save
                EntryEditDialog.NotSaved -> leave()
                else -> reduce { state.copy(dialog = null) }
            }

            EntryEditAction.Save -> save()

            EntryEditAction.Back -> when {
                state.saving -> Unit
                state.content != null && state.changed -> reduce { state.copy(dialog = EntryEditDialog.Leave) }
                else -> leave()
            }

            EntryEditAction.MessageShown -> reduce { state.copy(message = null) }
        }
    }

    private suspend fun Syntax<EntryEditState, Nothing>.load(id: EntryId) {
        try {
            val content = entries.content(dbKey, id)
            reduce { state.copy(loading = false, content = content, initial = content) }
        } catch (_: EntryFailure.EntryNotFound) {
            leave()
        } catch (failure: EntryFailure) {
            log.unexpected(TAG, failure)
            reduce { state.copy(loading = false, message = EntryEditMessage.LoadFailed) }
        }
    }

    private suspend fun Syntax<EntryEditState, Nothing>.confirm() {
        val content = state.content ?: return
        when (val dialog = state.dialog) {
            null -> Unit

            is EntryEditDialog.AddField -> {
                val problem = EntryValidator.nameProblem(content, dialog.name)
                if (problem != null) {
                    reduce { state.copy(dialog = dialog.copy(problem = problem)) }
                } else {
                    val field = EntryField(EntryValidator.normalizeName(dialog.name), value = "", protected = false)
                    reduce { state.edited { copy(ownFields = ownFields + field) }.copy(dialog = null) }
                }
            }

            is EntryEditDialog.RenameField -> {
                val problem = EntryValidator.nameProblem(content, dialog.name, renamedIndex = dialog.index)
                if (problem != null) {
                    reduce { state.copy(dialog = dialog.copy(problem = problem)) }
                } else {
                    val name = EntryValidator.normalizeName(dialog.name)
                    reduce {
                        state.edited {
                            copy(ownFields = ownFields.mapIndexed { i, field -> if (i == dialog.index) field.copy(key = name) else field })
                        }.copy(dialog = null, problems = state.problems.withoutName(dialog.index))
                    }
                }
            }

            is EntryEditDialog.DeleteField -> reduce {
                state.edited { copy(ownFields = ownFields.filterIndexed { i, _ -> i != dialog.index }) }
                    .withoutOwnField(dialog.index)
                    .copy(dialog = null)
            }

            EntryEditDialog.Preset -> reduce { state.copy(dialog = null) }

            EntryEditDialog.Leave, EntryEditDialog.NotSaved -> leave()
        }
    }

    private suspend fun Syntax<EntryEditState, Nothing>.save() {
        val content = state.content ?: return
        if (state.saving) return

        reduce { state.copy(saving = true, problems = emptyList()) }
        try {
            if (entryId == null) {
                editor.create(dbKey, GroupId(checkNotNull(groupId)), content)
            } else {
                editor.update(dbKey, EntryId(entryId), content)
            }
            leave()
        } catch (failure: EntryFailure) {
            failed(failure)
        }
    }

    private suspend fun Syntax<EntryEditState, Nothing>.failed(failure: EntryFailure) {
        when (failure) {
            is EntryFailure.InvalidFields -> reduce { state.copy(saving = false, problems = failure.problems) }

            // Made, but not in the file. Staying would not help: a second save of a new entry would make a
            // second entry - the first one's id is not known here (pass-docs, Step 38, decision 13)
            is EntryFailure.NotSaved -> reduce { state.copy(saving = false, dialog = EntryEditDialog.NotSaved) }

            // Nothing to save into any more; the screen behind reads what is there
            is EntryFailure.EntryNotFound, is EntryFailure.GroupNotFound -> leave()

            is EntryFailure.Unexpected -> {
                log.unexpected(TAG, failure)
                reduce { state.copy(saving = false, message = EntryEditMessage.Unexpected) }
            }
        }
    }

    private suspend fun leave() {
        navigator.navigate(MviNavEvent.NavigateBack)
    }

    private companion object {
        const val TAG = "EntryEditProcessor"
    }
}

/** The form takes changes: it is read and nothing is under way. */
private val EntryEditState.editable: Boolean get() = content != null && !saving

private fun EntryEditState.ownField(index: Int): EntryField? = content?.ownFields?.getOrNull(index)

private inline fun EntryEditState.edited(change: EntryContent.() -> EntryContent): EntryEditState =
    if (content == null || saving) this else copy(content = content.change())

/** The own field at [index] is gone: what is said about the fields after it moves up by one. */
private fun EntryEditState.withoutOwnField(index: Int): EntryEditState = copy(
    shownOwn = shownOwn.filter { it != index }.map { if (it > index) it - 1 else it }.toSet(),
    problems = problems.mapNotNull { problem ->
        when {
            problem !is FieldProblem.BadName -> problem
            problem.index == index -> null
            problem.index > index -> problem.copy(index = problem.index - 1)
            else -> problem
        }
    },
)

private fun List<FieldProblem>.withoutName(index: Int) = filterNot { it is FieldProblem.BadName && it.index == index }

private fun EntryEditDialog.typed(name: String): EntryEditDialog = when (this) {
    is EntryEditDialog.AddField -> copy(name = name, problem = null)
    is EntryEditDialog.RenameField -> copy(name = name, problem = null)
    else -> this
}
