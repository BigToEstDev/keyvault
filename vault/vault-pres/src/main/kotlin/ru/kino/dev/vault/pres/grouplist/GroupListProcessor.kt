package ru.kino.dev.vault.pres.grouplist

import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ru.kino.dev.core.CoreException
import ru.kino.dev.log.ErrorLog
import ru.kino.dev.mvi.textIntent
import ru.kino.dev.navigation.LockedRoute
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.Navigator
import ru.kino.dev.navigation.VaultRootRoute
import ru.kino.dev.vault.VaultDatabaseRepository
import ru.kino.dev.vault.group.GroupEditor
import ru.kino.dev.vault.group.GroupFailure
import ru.kino.dev.vault.group.GroupId
import ru.kino.dev.vault.group.GroupRepository
import ru.kino.dev.vault.group.VaultTree
import ru.kino.dev.vault.pres.navigation.EntryEditRoute
import ru.kino.dev.vault.pres.navigation.GroupRoute

/**
 * @param loading the tree is not read yet - the skeleton is shown
 * @param path from the root down to this group; empty until the tree is read
 * @param entryCount entries in this group and below it; at the root - the whole database
 * @param query typed into the search field; nothing searches yet - pass-docs, Step 38
 * @param message for the snackbar, until it takes it
 */
data class GroupListState(
    val loading: Boolean = true,
    val path: List<Crumb> = emptyList(),
    val entryCount: Int = 0,
    val groups: List<GroupItem> = emptyList(),
    val query: String = "",
    val fabExpanded: Boolean = false,
    val dialog: GroupDialog? = null,
    val locking: Boolean = false,
    val message: GroupListMessage? = null,
) {
    val isRoot: Boolean get() = path.size <= 1
}

/**
 * One group of the open database and the groups inside it - the root, or any group below it (pass-docs,
 * `docs/android/screens/vault/vault-our.md`).
 *
 * The tree is read anew every time the screen is shown: a change made one level down, or a group deleted
 * there, has to show here on the way back. Changes go through [GroupEditor] - checked, made, written to the
 * file - and the tree is read again after each.
 *
 * Locking puts the locked screen of `welcome` on top: this screen stays under it, and unlocking comes back
 * here.
 *
 * @param groupId null for the root
 */
@HiltViewModel(assistedFactory = GroupListProcessor.Factory::class)
class GroupListProcessor @AssistedInject constructor(
    @Assisted("dbKey") private val dbKey: String,
    @Assisted("groupId") private val groupId: String?,
    private val groups: GroupRepository,
    private val editor: GroupEditor,
    private val database: VaultDatabaseRepository,
    private val navigator: Navigator,
    private val log: ErrorLog,
) : ViewModel(), OrbitContainerHost<GroupListState, GroupListState, Nothing> {

    @AssistedFactory
    interface Factory {
        fun create(@Assisted("dbKey") dbKey: String, @Assisted("groupId") groupId: String?): GroupListProcessor
    }

    override val container = orbitContainer<GroupListState, Nothing>(GroupListState())

    fun action(action: GroupListAction): Job = when (action) {
        // Typed text: the state is updated before the field draws again, or the cursor jumps
        is GroupListAction.QueryChanged -> textIntent { reduce { state.copy(query = action.query) } }

        is GroupListAction.DialogNameChanged -> textIntent {
            reduce { state.copy(dialog = state.dialog?.typed(action.name)) }
        }

        else -> intent { handle(action) }
    }

    private suspend fun Syntax<GroupListState, Nothing>.handle(action: GroupListAction) {
        when (action) {
            is GroupListAction.QueryChanged, is GroupListAction.DialogNameChanged -> Unit // in action()

            GroupListAction.Load -> load()

            is GroupListAction.OpenGroup -> navigator.navigate(MviNavEvent.NavigateTo(GroupRoute(dbKey, action.id.value)))

            is GroupListAction.OpenCrumb -> openCrumb(action.index)

            GroupListAction.Back -> navigator.navigate(MviNavEvent.NavigateBack)

            GroupListAction.ToggleFab -> reduce { state.copy(fabExpanded = !state.fabExpanded) }

            GroupListAction.CreateGroup -> if (!state.loading) {
                reduce { state.copy(fabExpanded = false, dialog = GroupDialog.Create()) }
            }

            GroupListAction.CreateEntry -> createEntry()

            is GroupListAction.RenameGroup -> state.groups.find { it.id == action.id }?.let { group ->
                reduce { state.copy(dialog = GroupDialog.Rename(group.id, group.name)) }
            }

            is GroupListAction.DeleteGroup -> state.groups.find { it.id == action.id }?.let { group ->
                reduce { state.copy(dialog = GroupDialog.Delete(group.id, group.name)) }
            }

            GroupListAction.DialogConfirm -> confirm()

            GroupListAction.DialogDismiss -> if (state.dialog?.saving == false) {
                reduce { state.copy(dialog = null) }
            }

            GroupListAction.MessageShown -> reduce { state.copy(message = null) }

            GroupListAction.Lock -> lock()
        }
    }

    private suspend fun Syntax<GroupListState, Nothing>.load() {
        val tree = try {
            groups.tree(dbKey)
        } catch (failure: GroupFailure) {
            if (failure is GroupFailure.Unexpected) log.unexpected(TAG, failure)
            reduce { state.copy(loading = false, message = GroupListMessage.LoadFailed) }
            return
        }

        val id = groupId?.let(::GroupId) ?: tree.rootId
        if (tree.group(id) == null) {
            // Deleted elsewhere, or merged away: there is nothing to show here any more
            toRoot()
            return
        }
        reduce { state.shown(tree, id) }
    }

    private fun GroupListState.shown(tree: VaultTree, id: GroupId) = copy(
        loading = false,
        path = tree.path(id).map { Crumb(it.id, it.name) },
        entryCount = tree.entryCount(id),
        groups = tree.subgroups(id).map { GroupItem(it.id, it.name, tree.entryCount(it.id)) },
    )

    private suspend fun Syntax<GroupListState, Nothing>.openCrumb(index: Int) {
        val path = state.path
        if (index !in path.indices || index == path.lastIndex) return

        // The path is the back stack: every group was opened from the one before it, so going to a part of
        // it is going back to it, and what was above is dropped
        val route: Any = if (index == 0) VaultRootRoute(dbKey) else GroupRoute(dbKey, path[index].id.value)
        navigator.navigate(MviNavEvent.NavigateBackTo(route))
    }

    private suspend fun toRoot() {
        navigator.navigate(MviNavEvent.NavigateBackTo(VaultRootRoute(dbKey)))
    }

    private suspend fun Syntax<GroupListState, Nothing>.createEntry() {
        val current = state.path.lastOrNull() ?: return
        reduce { state.copy(fabExpanded = false) }
        navigator.navigate(MviNavEvent.NavigateTo(EntryEditRoute(dbKey, parentGroupId = current.id.value)))
    }

    private suspend fun Syntax<GroupListState, Nothing>.confirm() {
        val dialog = state.dialog ?: return
        if (dialog.saving) return
        val current = state.path.lastOrNull()?.id ?: return

        reduce { state.copy(dialog = dialog.underWay()) }
        try {
            when (dialog) {
                is GroupDialog.Create -> editor.create(dbKey, current, dialog.name)
                is GroupDialog.Rename -> editor.rename(dbKey, dialog.groupId, dialog.name)
                is GroupDialog.Delete -> editor.delete(dbKey, dialog.groupId)
            }
            reduce { state.copy(dialog = null) }
            load()
        } catch (failure: GroupFailure) {
            failed(failure)
        }
    }

    private suspend fun Syntax<GroupListState, Nothing>.failed(failure: GroupFailure) {
        when (failure) {
            is GroupFailure.NameEmpty -> reduce { state.copy(dialog = state.dialog?.refused(NameProblem.Empty)) }

            is GroupFailure.NameTooLong -> reduce { state.copy(dialog = state.dialog?.refused(NameProblem.TooLong)) }

            is GroupFailure.NameTaken -> reduce { state.copy(dialog = state.dialog?.refused(NameProblem.Taken)) }

            // Made, but not in the file: the list shows the change, the snackbar says it is not written
            is GroupFailure.NotSaved -> {
                reduce { state.copy(dialog = null, message = GroupListMessage.NotSaved) }
                load()
            }

            // The group asked about is gone; reading the tree again shows what is there - or goes to the
            // root, when the gone one is this group
            is GroupFailure.GroupNotFound -> {
                reduce { state.copy(dialog = null) }
                load()
            }

            is GroupFailure.Unexpected -> {
                log.unexpected(TAG, failure)
                reduce { state.copy(dialog = null, message = GroupListMessage.Unexpected) }
            }
        }
    }

    private suspend fun Syntax<GroupListState, Nothing>.lock() {
        if (state.locking) return

        reduce { state.copy(locking = true, fabExpanded = false) }
        try {
            database.lock(dbKey)
            reduce { state.copy(locking = false) }
            navigator.navigate(MviNavEvent.NavigateTo(LockedRoute(dbKey)))
        } catch (e: CoreException) {
            // The vault repositories throw what the core throws; no screen answer of its own yet
            log.unexpected(TAG, e)
            reduce { state.copy(locking = false, message = GroupListMessage.Unexpected) }
        }
    }

    private companion object {
        const val TAG = "GroupListProcessor"
    }
}

private fun GroupDialog.typed(name: String): GroupDialog = when (this) {
    is GroupDialog.Create -> if (saving) this else copy(name = name, problem = null)
    is GroupDialog.Rename -> if (saving) this else copy(name = name, problem = null)
    is GroupDialog.Delete -> this
}

private fun GroupDialog.underWay(): GroupDialog = when (this) {
    is GroupDialog.Create -> copy(saving = true, problem = null)
    is GroupDialog.Rename -> copy(saving = true, problem = null)
    is GroupDialog.Delete -> copy(saving = true)
}

/** Back to typing, with the reason under the field. A delete has no field, so it only stops waiting. */
private fun GroupDialog.refused(problem: NameProblem): GroupDialog = when (this) {
    is GroupDialog.Create -> copy(saving = false, problem = problem)
    is GroupDialog.Rename -> copy(saving = false, problem = problem)
    is GroupDialog.Delete -> copy(saving = false)
}
