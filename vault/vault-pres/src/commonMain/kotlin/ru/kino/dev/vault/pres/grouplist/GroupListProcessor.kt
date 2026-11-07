package ru.kino.dev.vault.pres.grouplist

import androidx.lifecycle.ViewModel
import java.io.IOException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ru.kino.dev.log.ErrorLog
import ru.kino.dev.mvi.textIntent
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.Navigator
import ru.kino.dev.navigation.SettingsRoute
import ru.kino.dev.navigation.TrashRoute
import ru.kino.dev.navigation.VaultRootRoute
import ru.kino.dev.vault.changes.VaultChanges
import ru.kino.dev.vault.entries.EntrySettingsRepository
import ru.kino.dev.vault.entry.EntryEditor
import ru.kino.dev.vault.entry.EntryFailure
import ru.kino.dev.vault.entry.EntryRepository
import ru.kino.dev.vault.entry.EntrySummary
import ru.kino.dev.vault.filter.CreationOrder
import ru.kino.dev.vault.filter.VaultFilter
import ru.kino.dev.vault.filter.VaultFilterRepository
import ru.kino.dev.vault.filter.Visibility
import ru.kino.dev.vault.group.EntryCount
import ru.kino.dev.vault.group.EntryId
import ru.kino.dev.vault.group.GroupEditor
import ru.kino.dev.vault.group.GroupFailure
import ru.kino.dev.vault.group.GroupId
import ru.kino.dev.vault.group.GroupRepository
import ru.kino.dev.vault.group.VaultTree
import ru.kino.dev.vault.lock.AutoLock
import ru.kino.dev.vault.pres.common.ChangeGate
import ru.kino.dev.vault.pres.common.SEARCH_DELAY
import ru.kino.dev.vault.pres.common.protectionOrDefault
import ru.kino.dev.vault.pres.navigation.EntryCreateRoute
import ru.kino.dev.vault.pres.navigation.EntryEditRoute
import ru.kino.dev.vault.pres.navigation.EntryReadRoute
import ru.kino.dev.vault.pres.navigation.GroupRoute
import ru.kino.dev.vault.search.VaultSearch
import ru.kino.dev.vault.sync.SaveState
import ru.kino.dev.vault.sync.SaveStatus
import ru.kino.dev.vault.sync.SyncRequests
import ru.kino.dev.vault.trash.TrashRepository
import ru.kino.dev.vault.trash.deletesToBin

/**
 * One group of the open database, the groups inside it and then its entries - the root, or any group below
 * it (pass-docs, `docs/android/screens/vault/vault-our.md`).
 *
 * The tree and the entries are read anew every time the screen is shown: a change made one level down, or
 * in an entry, has to show here on the way back. Changes go through [GroupEditor] and [EntryEditor] -
 * checked, made, written to the file - and everything is read again after each. A change made elsewhere while the
 * screen is shown - by the entry beside it, in a container - is read again too ([ChangeGate], pass-docs, Step 98).
 *
 * Typing into the search field searches this group and everything below it once the typing stops for
 * [SEARCH_DELAY]; an empty field is the group again. The search lives while the screen stays put: going
 * into a group or an entry, back, or locking, clears it (`vault-our.md`, "Время жизни Поиска"). The filter
 * is read from the device every time the screen is shown, and kept there when applied.
 *
 * The root hands the database to [AutoLock]: from then on it locks after a while without a touch, and the
 * app puts the locked screen of `welcome` up with `vault` gone - unlocking starts at the root again
 * (pass-docs, Step 46).
 *
 * @param groupId null for the root
 */
class GroupListProcessor(
    private val dbKey: String,
    private val groupId: String?,
    private val groups: GroupRepository,
    private val editor: GroupEditor,
    private val entries: EntryRepository,
    private val entryEditor: EntryEditor,
    private val autoLock: AutoLock,
    private val vaultSearch: VaultSearch,
    private val filters: VaultFilterRepository,
    private val entrySettings: EntrySettingsRepository,
    private val trash: TrashRepository,
    private val changes: VaultChanges,
    private val saveStatus: SaveStatus,
    private val syncRequests: SyncRequests,
    private val navigator: Navigator,
    private val log: ErrorLog,
) : ViewModel(), OrbitContainerHost<GroupListState, GroupListState, Nothing>, GroupListActions {

    private val gate = ChangeGate(changes, dbKey)

    override val container = orbitContainer<GroupListState, Nothing>(GroupListState(isRoot = groupId == null)) {
        // While the screen is shown: a change made beside it is read again; the root says what waits in memory
        repeatOnSubscription {
            merge(
                gate.elsewhere.map { Shown.Changed },
                saveStatus.state(dbKey).filter { groupId == null }.map { Shown.Status(it) },
            ).collect { shown ->
                when (shown) {
                    Shown.Changed -> load()
                    is Shown.Status -> reduce {
                        state.copy(unsavedChangedElsewhere = shown.state == SaveState.ChangedElsewhere)
                    }
                }
            }
        }
    }

    /** The search waiting for the typing to stop, or for its answer; the next letter cancels it. */
    private var searchJob: Job? = null

    init {
        // The root is where `vault` begins - after opening and after every unlock
        if (groupId == null) autoLock.watch(dbKey)
    }

    override fun onLoad() {
        intent { load() }
    }

    override fun onQueryChange(query: String) {
        // Typed text: the state is updated before the field draws again, or the cursor jumps
        textIntent {
            reduce {
                if (query.isBlank()) {
                    state.copy(query = query, searching = false, found = null)
                } else {
                    state.copy(query = query, searching = true)
                }
            }
        }
        searchJob?.cancel()
        if (query.isBlank()) return

        searchJob = intent {
            delay(SEARCH_DELAY)
            search(query)
        }
    }

    override fun onOpenGroup(id: GroupId) {
        intent {
            clearSearch()
            navigator.navigate(MviNavEvent.NavigateTo(GroupRoute(dbKey, id.value)))
        }
    }

    override fun onOpenCrumb(index: Int) {
        intent { openCrumb(index) }
    }

    override fun onBack() {
        intent {
            clearSearch()
            navigator.navigate(MviNavEvent.NavigateBack)
        }
    }

    override fun onToggleFab() {
        intent { reduce { state.copy(fabExpanded = !state.fabExpanded) } }
    }

    override fun onCloseFab() {
        intent { reduce { state.copy(fabExpanded = false) } }
    }

    override fun onOpenFilters() {
        intent { reduce { state.copy(filterDraft = state.filter, fabExpanded = false) } }
    }

    override fun onCloseFilters() {
        intent { if (!state.savingFilter) reduce { state.copy(filterDraft = null) } }
    }

    override fun onFilterVisibilityChange(visibility: Visibility) {
        intent { reduce { state.copy(filterDraft = state.filterDraft?.copy(visibility = visibility)) } }
    }

    override fun onFilterOrderChange(order: CreationOrder) {
        intent { reduce { state.copy(filterDraft = state.filterDraft?.copy(order = order)) } }
    }

    override fun onApplyFilters() {
        intent { state.filterDraft?.let { applyFilter(it) } }
    }

    override fun onResetFilters() {
        intent { if (state.filtersOpen) applyFilter(VaultFilter.DEFAULT) }
    }

    override fun onCreateGroup() {
        intent {
            if (!state.loading) reduce { state.copy(fabExpanded = false, dialog = GroupDialog.Create()) }
        }
    }

    override fun onCreateEntry() {
        intent { createEntry() }
    }

    override fun onOpenSettings() {
        intent { navigator.navigate(MviNavEvent.NavigateTo(SettingsRoute(dbKey))) }
    }

    override fun onOpenTrash() {
        intent {
            clearSearch()
            navigator.navigate(MviNavEvent.NavigateTo(TrashRoute(dbKey)))
        }
    }

    override fun onOpenEntry(id: EntryId) {
        intent {
            clearSearch()
            navigator.navigate(MviNavEvent.NavigateTo(EntryReadRoute(dbKey, id.value)))
        }
    }

    override fun onEditEntry(id: EntryId) {
        intent {
            clearSearch()
            navigator.navigate(MviNavEvent.NavigateTo(EntryEditRoute(dbKey, id.value)))
        }
    }

    override fun onDeleteEntry(id: EntryId) {
        intent {
            state.entries.find { it.id == id }?.let { entry ->
                val toBin = trash.deletesToBin(dbKey)
                reduce { state.copy(dialog = GroupDialog.DeleteEntry(entry.id, entry.title, toBin)) }
            }
        }
    }

    override fun onRenameGroup(id: GroupId) {
        intent {
            state.groups.find { it.id == id }?.let { group ->
                reduce { state.copy(dialog = GroupDialog.Rename(group.id, group.name)) }
            }
        }
    }

    override fun onDeleteGroup(id: GroupId) {
        intent { state.groups.find { it.id == id }?.let { askDelete(it.id, it.name) } }
    }

    override fun onRenameCurrent() {
        intent {
            state.current?.let { group -> reduce { state.copy(dialog = GroupDialog.Rename(group.id, group.name)) } }
        }
    }

    override fun onDeleteCurrent() {
        intent { state.current?.let { askDelete(it.id, it.name) } }
    }

    override fun onDialogNameChange(name: String) {
        textIntent { reduce { state.copy(dialog = state.dialog?.typed(name)) } }
    }

    override fun onDialogConfirm() {
        intent { confirm() }
    }

    override fun onDialogDismiss() {
        intent { if (state.dialog?.saving == false) reduce { state.copy(dialog = null) } }
    }

    override fun onMessageShown() {
        intent { reduce { state.copy(message = null) } }
    }

    override fun onLock() {
        intent { lock() }
    }

    override fun onSynchronize() {
        intent { if (state.unsavedChangedElsewhere) syncRequests.synchronize(dbKey) }
    }

    private suspend fun Syntax<GroupListState, Nothing>.askDelete(id: GroupId, name: String) {
        val toBin = trash.deletesToBin(dbKey)
        reduce { state.copy(dialog = GroupDialog.Delete(id, name, toBin)) }
    }

    private suspend fun Syntax<GroupListState, Nothing>.load() {
        gate.reading()
        val filter = filters.filter.first()
        val tree = try {
            groups.tree(dbKey)
        } catch (failure: GroupFailure) {
            if (failure is GroupFailure.Unexpected) log.unexpected(TAG, failure)
            reduce { state.copy(loading = false, filter = filter, message = GroupListMessage.LoadFailed) }
            return
        }

        val id = groupId?.let(::GroupId) ?: tree.rootId
        if (tree.group(id) == null) {
            // Deleted elsewhere, or merged away: there is nothing to show here any more
            toRoot()
            return
        }

        val inside = try {
            entries.entries(dbKey, id)
        } catch (failure: EntryFailure) {
            if (failure is EntryFailure.Unexpected) log.unexpected(TAG, failure)
            reduce { state.copy(loading = false, filter = filter, message = GroupListMessage.LoadFailed) }
            return
        }
        // Read anew too: it may come back from the settings changed
        val userNameHidden = entrySettings.protectionOrDefault(dbKey, log, TAG).userName
        reduce { state.shown(tree, id, inside).copy(filter = filter, userNameHidden = userNameHidden) }

        // What was found may have changed with what was just read - a group deleted, an entry renamed
        if (state.isSearch) search(state.query)
    }

    private fun GroupListState.shown(tree: VaultTree, id: GroupId, entries: List<EntrySummary>) = copy(
        loading = false,
        path = tree.path(id).map { Crumb(it.id, it.name) },
        entryCount = tree.entryCount(id),
        listed = ListSource(
            groups = tree.subgroups(id).map { GroupItem(it.id, it.name, tree.entryCount(it.id)) },
            entries = entries,
        ),
    )

    private suspend fun Syntax<GroupListState, Nothing>.search(term: String) {
        // The tree is not read yet: load() runs the search when it is
        val scope = state.path.lastOrNull()?.id ?: return
        val result = try {
            vaultSearch.search(dbKey, scope, term)
        } catch (failure: GroupFailure) {
            searchFailed(term, failure, unexpected = failure is GroupFailure.Unexpected)
            return
        } catch (failure: EntryFailure) {
            searchFailed(term, failure, unexpected = failure is EntryFailure.Unexpected)
            return
        }

        // The calls may finish in any order: only the answer to what is in the field now is shown
        if (result.term != state.query) return
        reduce {
            state.copy(
                searching = false,
                found = ListSource(
                    groups = result.groups.map {
                        GroupItem(
                            id = it.id,
                            name = it.name,
                            entryCount = result.entryCounts[it.id] ?: EntryCount.None,
                            location = it.parentId?.let(result.locations::get),
                        )
                    },
                    entries = result.entries,
                    locations = result.locations,
                ),
            )
        }
    }

    private suspend fun Syntax<GroupListState, Nothing>.searchFailed(
        term: String,
        failure: Exception,
        unexpected: Boolean,
    ) {
        if (unexpected) log.unexpected(TAG, failure)
        if (term == state.query) reduce { state.copy(searching = false, message = GroupListMessage.SearchFailed) }
    }

    private suspend fun Syntax<GroupListState, Nothing>.clearSearch() {
        searchJob?.cancel()
        reduce { state.copy(query = "", searching = false, found = null) }
    }

    private suspend fun Syntax<GroupListState, Nothing>.applyFilter(filter: VaultFilter) {
        if (state.savingFilter) return

        reduce { state.copy(savingFilter = true) }
        val message = try {
            filters.save(filter)
            null
        } catch (e: IOException) {
            // In effect all the same - only not remembered for the next launch
            log.unexpected(TAG, e)
            GroupListMessage.FilterNotSaved
        }
        reduce {
            state.copy(filter = filter, filterDraft = null, savingFilter = false, message = message ?: state.message)
        }
    }

    private suspend fun Syntax<GroupListState, Nothing>.openCrumb(index: Int) {
        val path = state.path
        if (index !in path.indices || index == path.lastIndex) return

        // The path is the back stack: every group was opened from the one before it, so going to a part of
        // it is going back to it, and what was above is dropped
        val route: Any = if (index == 0) VaultRootRoute(dbKey) else GroupRoute(dbKey, path[index].id.value)
        clearSearch()
        navigator.navigate(MviNavEvent.NavigateBackTo(route))
    }

    private suspend fun toRoot() {
        navigator.navigate(MviNavEvent.NavigateBackTo(VaultRootRoute(dbKey)))
    }

    private suspend fun Syntax<GroupListState, Nothing>.createEntry() {
        val current = state.path.lastOrNull() ?: return
        reduce { state.copy(fabExpanded = false) }
        clearSearch()
        navigator.navigate(MviNavEvent.NavigateTo(EntryCreateRoute(dbKey, groupId = current.id.value)))
    }

    private suspend fun Syntax<GroupListState, Nothing>.confirm() {
        val dialog = state.dialog ?: return
        if (dialog.saving) return
        val current = state.path.lastOrNull()?.id ?: return

        reduce { state.copy(dialog = dialog.underWay()) }
        // Its own change: read again, or gone to the parent, here - not by the change as well
        gate.own {
            try {
                when (dialog) {
                    is GroupDialog.Create -> editor.create(dbKey, current, dialog.name)
                    is GroupDialog.Rename -> editor.rename(dbKey, dialog.groupId, dialog.name)
                    is GroupDialog.Delete -> editor.delete(dbKey, dialog.groupId)
                    is GroupDialog.DeleteEntry -> entryEditor.delete(dbKey, dialog.entryId)
                }
                reduce { state.copy(dialog = null) }
                // This group is in the recycle bin now: nothing to show here, the screen goes to its parent
                if (dialog.isDeleting(current)) openCrumb(state.path.lastIndex - 1) else load()
            } catch (failure: GroupFailure) {
                failed(failure)
            } catch (failure: EntryFailure) {
                failed(failure)
            }
        }
    }

    private suspend fun Syntax<GroupListState, Nothing>.failed(failure: EntryFailure) {
        when (failure) {
            // In the recycle bin, but not in the file: the list shows it gone, the snackbar says it is not written
            is EntryFailure.NotSaved -> {
                reduce { state.copy(dialog = null, message = GroupListMessage.NotSaved) }
                load()
            }

            // Gone already - deleted or merged away; reading again shows what is there
            is EntryFailure.EntryNotFound -> {
                reduce { state.copy(dialog = null) }
                load()
            }

            // Not what deleting an entry throws
            is EntryFailure.InvalidFields, is EntryFailure.GroupNotFound, is EntryFailure.Unexpected -> {
                log.unexpected(TAG, failure)
                reduce { state.copy(dialog = null, message = GroupListMessage.Unexpected) }
            }
        }
    }

    private suspend fun Syntax<GroupListState, Nothing>.failed(failure: GroupFailure) {
        when (failure) {
            is GroupFailure.NameEmpty -> reduce { state.copy(dialog = state.dialog?.refused(NameProblem.Empty)) }

            is GroupFailure.NameTooLong -> reduce { state.copy(dialog = state.dialog?.refused(NameProblem.TooLong)) }

            is GroupFailure.NameTaken -> reduce { state.copy(dialog = state.dialog?.refused(NameProblem.Taken)) }

            // Made, but not in the file: the list shows the change, the snackbar says it is not written. This
            // group deleted stays on screen: reading again would find it gone and go to the root, the snackbar
            // with it - the error is shown here (pass-docs, Step 50, item 1)
            is GroupFailure.NotSaved -> {
                val deletedHere = state.dialog?.isDeleting(state.path.lastOrNull()?.id) == true
                reduce { state.copy(dialog = null, message = GroupListMessage.NotSaved) }
                if (!deletedHere) load()
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
        // The locked screen is put up by the app, with `vault` gone under it; the refusal is logged by the lock
        if (!autoLock.lockNow()) {
            reduce { state.copy(locking = false, message = GroupListMessage.Unexpected) }
        }
    }

    private companion object {
        const val TAG = "GroupListProcessor"
    }
}

/** The dialog deletes the group [groupId] - the one shown, when it is that one. */
private fun GroupDialog.isDeleting(groupId: GroupId?): Boolean = this is GroupDialog.Delete && this.groupId == groupId

private fun GroupDialog.typed(name: String): GroupDialog = when (this) {
    is GroupDialog.Create -> if (saving) this else copy(name = name, problem = null)
    is GroupDialog.Rename -> if (saving) this else copy(name = name, problem = null)
    is GroupDialog.Delete, is GroupDialog.DeleteEntry -> this
}

private fun GroupDialog.underWay(): GroupDialog = when (this) {
    is GroupDialog.Create -> copy(saving = true, problem = null)
    is GroupDialog.Rename -> copy(saving = true, problem = null)
    is GroupDialog.Delete -> copy(saving = true)
    is GroupDialog.DeleteEntry -> copy(saving = true)
}

/** Back to typing, with the reason under the field. A delete has no field, so it only stops waiting. */
private fun GroupDialog.refused(problem: NameProblem): GroupDialog = when (this) {
    is GroupDialog.Create -> copy(saving = false, problem = problem)
    is GroupDialog.Rename -> copy(saving = false, problem = problem)
    is GroupDialog.Delete -> copy(saving = false)
    is GroupDialog.DeleteEntry -> copy(saving = false)
}

/** What the screen hears while shown. */
private sealed interface Shown {
    data object Changed : Shown

    data class Status(val state: SaveState) : Shown
}
