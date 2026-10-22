package ru.kino.dev.vault.pres.grouplist

import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
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
import ru.kino.dev.vault.entries.EntrySettingsRepository
import ru.kino.dev.vault.entry.EntryEditor
import ru.kino.dev.vault.entry.EntryFailure
import ru.kino.dev.vault.entry.EntryRepository
import ru.kino.dev.vault.entry.EntrySummary
import ru.kino.dev.vault.filter.VaultFilter
import ru.kino.dev.vault.filter.VaultFilterRepository
import ru.kino.dev.vault.filter.Visibility
import ru.kino.dev.vault.filter.applyTo
import ru.kino.dev.vault.group.EntryCount
import ru.kino.dev.vault.group.GroupEditor
import ru.kino.dev.vault.group.GroupFailure
import ru.kino.dev.vault.group.GroupId
import ru.kino.dev.vault.group.GroupRepository
import ru.kino.dev.vault.group.VaultTree
import ru.kino.dev.vault.lock.AutoLock
import ru.kino.dev.vault.pres.common.SEARCH_DELAY
import ru.kino.dev.vault.pres.common.protectionOrDefault
import ru.kino.dev.vault.pres.navigation.EntryCreateRoute
import ru.kino.dev.vault.pres.navigation.EntryEditRoute
import ru.kino.dev.vault.pres.navigation.EntryReadRoute
import ru.kino.dev.vault.pres.navigation.GroupRoute
import ru.kino.dev.vault.search.VaultSearch
import ru.kino.dev.vault.trash.TrashRepository
import ru.kino.dev.vault.trash.deletesToBin

/**
 * @param isRoot the screen is the root of the database - known from the route, before the tree is read, so
 * the top bar of a group below it never shows the root's while it loads (pass-docs, Step 40, fix 13)
 * @param loading the tree and the entries are not read yet - the skeleton is shown
 * @param path from the root down to this group; empty until the tree is read
 * @param entryCount entries of this group - its own, and with those below; at the root - the whole database
 * @param listed what is directly in this group, before the filter
 * @param query typed into the search field; not blank - the list is what it finds
 * @param searching a search waits for the typing to stop, or for its answer - the skeleton is shown
 * @param found what [query] finds in this group and below it, before the filter; null with no search
 * @param filter the filter in effect - as kept on the device
 * @param filterDraft the filter dialog is shown, with what is chosen in it so far
 * @param savingFilter the filter chosen is being kept - the dialog waits
 * @param userNameHidden the database hides user names (pass-docs, Step 65): none in the list, whatever the
 * appearance says (pass-docs, Step 56, item 5)
 * @param message for the snackbar, until it takes it
 */
data class GroupListState(
    val isRoot: Boolean = true,
    val loading: Boolean = true,
    val path: List<Crumb> = emptyList(),
    val entryCount: EntryCount = EntryCount.None,
    val listed: ListSource = ListSource(),
    val query: String = "",
    val searching: Boolean = false,
    val found: ListSource? = null,
    val filter: VaultFilter = VaultFilter.DEFAULT,
    val filterDraft: VaultFilter? = null,
    val savingFilter: Boolean = false,
    val fabExpanded: Boolean = false,
    val dialog: GroupDialog? = null,
    val locking: Boolean = false,
    val userNameHidden: Boolean = false,
    val message: GroupListMessage? = null,
) {
    // Worked out once for every state, not on every read
    private val shown = (found ?: listed).let { filter.applyTo(it.groups, it.entries) }

    /** The groups the list shows - of this group, or found by the search - in the order of the database. */
    val groups: List<GroupItem> = shown.groups

    /** The entries the list shows, after the groups: by when they were created, as the filter says. */
    val entries: List<EntryItem> = shown.entries.map {
        EntryItem(it.id, it.title, if (userNameHidden) "" else it.userName, found?.locations?.get(it.groupId))
    }

    val isSearch: Boolean get() = query.isNotBlank()

    /** The group shown here, once the tree is read; null at the root - the database, not a group. */
    val current: Crumb? get() = if (isRoot) null else path.lastOrNull()

    val filtersOpen: Boolean get() = filterDraft != null

    /** Nothing in the list. */
    val isEmpty: Boolean get() = groups.isEmpty() && entries.isEmpty()

    /** Why the list is empty, which says what to show in its place; null when it is not. */
    val emptyReason: EmptyReason?
        get() = when {
            !isEmpty -> null
            isSearch -> EmptyReason.NothingFound
            listed.groups.isEmpty() && listed.entries.isEmpty() -> EmptyReason.EmptyGroup
            filter.visibility == Visibility.GroupsOnly -> EmptyReason.NoGroups
            else -> EmptyReason.NoEntries
        }
}

/**
 * What a list is made of, before the filter: groups as their cards show them, entries as the core gives them.
 *
 * @param locations of a search: where the entries lie, by the group they are in
 */
data class ListSource(
    val groups: List<GroupItem> = emptyList(),
    val entries: List<EntrySummary> = emptyList(),
    val locations: Map<GroupId, List<String>> = emptyMap(),
)

enum class EmptyReason {
    /** The group holds nothing */
    EmptyGroup,

    /** The search finds nothing - or nothing the filter lets through */
    NothingFound,

    /** The group holds entries only, and the filter shows groups only */
    NoGroups,

    /** The group holds groups only, and the filter shows entries only */
    NoEntries,
}

/**
 * One group of the open database, the groups inside it and then its entries - the root, or any group below
 * it (pass-docs, `docs/android/screens/vault/vault-our.md`).
 *
 * The tree and the entries are read anew every time the screen is shown: a change made one level down, or
 * in an entry, has to show here on the way back. Changes go through [GroupEditor] and [EntryEditor] -
 * checked, made, written to the file - and everything is read again after each.
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
@HiltViewModel(assistedFactory = GroupListProcessor.Factory::class)
class GroupListProcessor @AssistedInject constructor(
    @Assisted("dbKey") private val dbKey: String,
    @Assisted("groupId") private val groupId: String?,
    private val groups: GroupRepository,
    private val editor: GroupEditor,
    private val entries: EntryRepository,
    private val entryEditor: EntryEditor,
    private val autoLock: AutoLock,
    private val vaultSearch: VaultSearch,
    private val filters: VaultFilterRepository,
    private val entrySettings: EntrySettingsRepository,
    private val trash: TrashRepository,
    private val navigator: Navigator,
    private val log: ErrorLog,
) : ViewModel(), OrbitContainerHost<GroupListState, GroupListState, Nothing> {

    @AssistedFactory
    interface Factory {
        fun create(@Assisted("dbKey") dbKey: String, @Assisted("groupId") groupId: String?): GroupListProcessor
    }

    override val container = orbitContainer<GroupListState, Nothing>(GroupListState(isRoot = groupId == null))

    /** The search waiting for the typing to stop, or for its answer; the next letter cancels it. */
    private var searchJob: Job? = null

    init {
        // The root is where `vault` begins - after opening and after every unlock
        if (groupId == null) autoLock.watch(dbKey)
    }

    fun action(action: GroupListAction): Job = when (action) {
        is GroupListAction.QueryChanged -> queryChanged(action.query)

        is GroupListAction.DialogNameChanged -> textIntent {
            reduce { state.copy(dialog = state.dialog?.typed(action.name)) }
        }

        else -> intent { handle(action) }
    }

    private suspend fun Syntax<GroupListState, Nothing>.handle(action: GroupListAction) {
        when (action) {
            is GroupListAction.QueryChanged, is GroupListAction.DialogNameChanged -> Unit // in action()

            GroupListAction.Load -> load()

            is GroupListAction.OpenGroup -> {
                clearSearch()
                navigator.navigate(MviNavEvent.NavigateTo(GroupRoute(dbKey, action.id.value)))
            }

            is GroupListAction.OpenCrumb -> openCrumb(action.index)

            GroupListAction.Back -> {
                clearSearch()
                navigator.navigate(MviNavEvent.NavigateBack)
            }

            GroupListAction.ToggleFab -> reduce { state.copy(fabExpanded = !state.fabExpanded) }

            GroupListAction.CloseFab -> reduce { state.copy(fabExpanded = false) }

            GroupListAction.OpenFilters -> reduce { state.copy(filterDraft = state.filter, fabExpanded = false) }

            GroupListAction.CloseFilters -> if (!state.savingFilter) reduce { state.copy(filterDraft = null) }

            is GroupListAction.FilterVisibilityChanged -> reduce {
                state.copy(filterDraft = state.filterDraft?.copy(visibility = action.visibility))
            }

            is GroupListAction.FilterOrderChanged -> reduce {
                state.copy(filterDraft = state.filterDraft?.copy(order = action.order))
            }

            GroupListAction.ApplyFilters -> state.filterDraft?.let { applyFilter(it) }

            GroupListAction.ResetFilters -> if (state.filtersOpen) applyFilter(VaultFilter.DEFAULT)

            GroupListAction.CreateGroup -> if (!state.loading) {
                reduce { state.copy(fabExpanded = false, dialog = GroupDialog.Create()) }
            }

            GroupListAction.CreateEntry -> createEntry()

            GroupListAction.OpenSettings -> navigator.navigate(MviNavEvent.NavigateTo(SettingsRoute(dbKey)))

            GroupListAction.OpenTrash -> {
                clearSearch()
                navigator.navigate(MviNavEvent.NavigateTo(TrashRoute(dbKey)))
            }

            is GroupListAction.OpenEntry -> {
                clearSearch()
                navigator.navigate(MviNavEvent.NavigateTo(EntryReadRoute(dbKey, action.id.value)))
            }

            is GroupListAction.EditEntry -> {
                clearSearch()
                navigator.navigate(MviNavEvent.NavigateTo(EntryEditRoute(dbKey, action.id.value)))
            }

            is GroupListAction.DeleteEntry -> state.entries.find { it.id == action.id }?.let { entry ->
                val toBin = trash.deletesToBin(dbKey)
                reduce { state.copy(dialog = GroupDialog.DeleteEntry(entry.id, entry.title, toBin)) }
            }

            is GroupListAction.RenameGroup -> state.groups.find { it.id == action.id }?.let { group ->
                reduce { state.copy(dialog = GroupDialog.Rename(group.id, group.name)) }
            }

            is GroupListAction.DeleteGroup -> state.groups.find { it.id == action.id }?.let { group ->
                val toBin = trash.deletesToBin(dbKey)
                reduce { state.copy(dialog = GroupDialog.Delete(group.id, group.name, toBin)) }
            }

            GroupListAction.RenameCurrent -> state.current?.let { group ->
                reduce { state.copy(dialog = GroupDialog.Rename(group.id, group.name)) }
            }

            GroupListAction.DeleteCurrent -> state.current?.let { group ->
                val toBin = trash.deletesToBin(dbKey)
                reduce { state.copy(dialog = GroupDialog.Delete(group.id, group.name, toBin)) }
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

    private fun queryChanged(query: String): Job {
        // Typed text: the state is updated before the field draws again, or the cursor jumps
        val typed = textIntent {
            reduce {
                if (query.isBlank()) {
                    state.copy(query = query, searching = false, found = null)
                } else {
                    state.copy(query = query, searching = true)
                }
            }
        }
        searchJob?.cancel()
        if (query.isBlank()) return typed

        return intent {
            delay(SEARCH_DELAY)
            search(query)
        }.also { searchJob = it }
    }

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
