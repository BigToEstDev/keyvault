package ru.kino.dev.vault.pres.grouplist

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import org.orbitmvi.orbit.compose.collectAsState
import ru.kino.dev.core.pres.R
import ru.kino.dev.ui.components.EmptySearchIllustration
import ru.kino.dev.ui.components.EmptyState
import ru.kino.dev.ui.components.ExpandableFab
import ru.kino.dev.ui.components.FabItem
import ru.kino.dev.ui.components.GradientBackground
import ru.kino.dev.ui.components.SnackbarMessage
import ru.kino.dev.vault.pres.grouplist.components.DebugLockFab
import ru.kino.dev.vault.pres.grouplist.components.EntryCard
import ru.kino.dev.vault.pres.grouplist.components.FilterDialog
import ru.kino.dev.vault.pres.grouplist.components.GroupCard
import ru.kino.dev.vault.pres.grouplist.components.GroupDialogs
import ru.kino.dev.vault.pres.grouplist.components.GroupListTopBar
import ru.kino.dev.vault.pres.grouplist.components.SkeletonCard

/** @param groupId null for the root of the database */
@Composable
internal fun GroupListScreen(
    dbKey: String,
    groupId: String?,
    processor: GroupListProcessor = hiltViewModel<GroupListProcessor, GroupListProcessor.Factory>(
        creationCallback = { factory -> factory.create(dbKey, groupId) },
    ),
) {
    val state by processor.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    // Every time the screen is shown: after "back" from a group below, or after unlocking, the tree may differ
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { processor.action(GroupListAction.Load) }

    BackHandler(enabled = state.fabExpanded) { processor.action(GroupListAction.CloseFab) }

    SnackbarMessage(state.message?.let { stringResource(it.text) }, snackbar) {
        processor.action(GroupListAction.MessageShown)
    }

    GradientBackground {
        Scaffold(
            // The backdrop is the gradient behind
            containerColor = Color.Transparent,
            topBar = {
                GroupListTopBar(
                    isRoot = state.isRoot,
                    path = state.path,
                    entryCount = state.entryCount,
                    query = state.query,
                    filterOn = !state.filter.isDefault,
                    onQueryChange = { processor.action(GroupListAction.QueryChanged(it)) },
                    onFilters = { processor.action(GroupListAction.OpenFilters) },
                    onBack = { processor.action(GroupListAction.Back) },
                    onCrumb = { processor.action(GroupListAction.OpenCrumb(it)) },
                )
            },
            // The fab is not in the scaffold (see below), so the snackbar is lifted over it here
            snackbarHost = { SnackbarHost(snackbar, Modifier.padding(bottom = FAB_CLEARANCE)) },
        ) { padding ->
            GroupListContent(state = state, padding = padding, action = processor::action)
        }

        // In the opposite corner from "add", away from the real buttons; the same margin as the fab
        DebugLockFab(
            locking = state.locking,
            onLock = { processor.action(GroupListAction.Lock) },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(FAB_MARGIN),
        )

        // Over everything but the fab: a tap past the open menu closes it and goes no further
        if (state.fabExpanded) {
            FabDismissLayer(onDismiss = { processor.action(GroupListAction.CloseFab) })
        }

        // Out of the scaffold's slot to be above the layer that closes it
        ExpandableFab(
            expanded = state.fabExpanded,
            onToggle = { processor.action(GroupListAction.ToggleFab) },
            items = listOf(
                FabItem(Icons.Filled.Key, stringResource(R.string.vault_create_entry)) {
                    processor.action(GroupListAction.CreateEntry)
                },
                FabItem(Icons.Filled.CreateNewFolder, stringResource(R.string.vault_create_group)) {
                    processor.action(GroupListAction.CreateGroup)
                },
            ),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(FAB_MARGIN),
        )

        state.filterDraft?.let { draft ->
            FilterDialog(
                draft = draft,
                saving = state.savingFilter,
                onVisibility = { processor.action(GroupListAction.FilterVisibilityChanged(it)) },
                onOrder = { processor.action(GroupListAction.FilterOrderChanged(it)) },
                onApply = { processor.action(GroupListAction.ApplyFilters) },
                onReset = { processor.action(GroupListAction.ResetFilters) },
                onClose = { processor.action(GroupListAction.CloseFilters) },
            )
        }

        GroupDialogs(
            dialog = state.dialog,
            onNameChange = { processor.action(GroupListAction.DialogNameChanged(it)) },
            onConfirm = { processor.action(GroupListAction.DialogConfirm) },
            onDismiss = { processor.action(GroupListAction.DialogDismiss) },
        )
    }
}

/** Transparent, the whole screen: takes the tap - nothing under it gets it - and closes the menu. */
@Composable
private fun FabDismissLayer(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClickLabel = stringResource(R.string.fab_close),
                onClick = onDismiss,
            ),
    )
}

@Composable
private fun GroupListContent(state: GroupListState, padding: PaddingValues, action: (GroupListAction) -> Unit) {
    val empty = state.emptyReason
    when {
        // The search shows the skeleton too: while the typing goes on, and while the core looks
        state.loading || state.searching -> LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            userScrollEnabled = false,
        ) {
            items(SKELETON_ROWS) { SkeletonCard() }
        }

        empty != null -> Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = EMPTY_ALIGNMENT,
        ) {
            GroupListEmpty(reason = empty, query = state.query, filterOn = !state.filter.isDefault)
        }

        else -> LazyColumn(
            modifier = Modifier.padding(padding),
            // Room under the last card for the buttons, so they never cover it
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = FAB_CLEARANCE),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.groups, key = { it.id.value }) { group ->
                GroupCard(
                    group = group,
                    location = locationText(group.location, fromRoot = state.isRoot),
                    onOpen = { action(GroupListAction.OpenGroup(group.id)) },
                    onEdit = { action(GroupListAction.RenameGroup(group.id)) },
                    onDelete = { action(GroupListAction.DeleteGroup(group.id)) },
                )
            }
            items(state.entries, key = { it.id.value }) { entry ->
                EntryCard(
                    entry = entry,
                    location = locationText(entry.location, fromRoot = state.isRoot),
                    onOpen = { action(GroupListAction.OpenEntry(entry.id)) },
                    onEdit = { action(GroupListAction.EditEntry(entry.id)) },
                    onDelete = { action(GroupListAction.DeleteEntry(entry.id)) },
                )
            }
        }
    }
}

/**
 * Where a card found by a search lies, as one line: the names from the group searched in, joined. At the root
 * the first is the database, which a root without a name shows as the top bar does - "Database".
 */
@Composable
private fun locationText(names: List<String>?, fromRoot: Boolean): String? {
    if (names == null) return null
    val fallback = stringResource(R.string.entry_database_fallback)
    return names
        .mapIndexed { index, name -> if (index == 0 && fromRoot) name.ifBlank { fallback } else name }
        .joinToString(stringResource(R.string.vault_location_separator))
}

/**
 * In place of the list when there is nothing in it: what the emptiness means, and the way out if any.
 *
 * A search that found nothing says what was looked for, by which fields, and what to check - the spelling,
 * and the filter too when one is on (Step 40, item 11).
 */
@Composable
private fun GroupListEmpty(reason: EmptyReason, query: String, filterOn: Boolean) {
    when (reason) {
        EmptyReason.EmptyGroup -> EmptyState(
            title = stringResource(R.string.vault_group_empty),
            hint = stringResource(R.string.vault_empty_hint),
        )

        EmptyReason.NothingFound -> EmptyState(
            title = stringResource(R.string.vault_search_nothing_found, query.trim()),
            // By which fields, then what to check: the filter too, when one is on
            hint = stringResource(R.string.vault_search_hint_fields) + "\n" + stringResource(
                if (filterOn) R.string.vault_search_check_spelling_and_filter else R.string.vault_search_check_spelling,
            ),
            illustration = { EmptySearchIllustration() },
        )

        EmptyReason.NoGroups -> EmptyState(title = stringResource(R.string.vault_filter_no_groups))

        EmptyReason.NoEntries -> EmptyState(title = stringResource(R.string.vault_filter_no_entries))
    }
}

private val GroupListMessage.text: Int
    get() = when (this) {
        GroupListMessage.NotSaved -> R.string.vault_message_not_saved
        GroupListMessage.LoadFailed -> R.string.vault_message_load_failed
        GroupListMessage.SearchFailed -> R.string.vault_message_search_failed
        GroupListMessage.FilterNotSaved -> R.string.vault_message_filter_not_saved
        GroupListMessage.Unexpected -> R.string.error_unexpected
    }

/** The empty state a little above the middle: the eye lands there, and the buttons take the bottom. */
private val EMPTY_ALIGNMENT = BiasAlignment(horizontalBias = 0f, verticalBias = -0.3f)

/** Cards drawn while the tree is read - about a screenful. */
private const val SKELETON_ROWS = 6

/** Room the buttons take at the bottom: under the last card, and under the snackbar. */
private val FAB_CLEARANCE = 96.dp

/** From the fabs to the edges of the screen - what the scaffold kept for its own fab. */
private val FAB_MARGIN = 16.dp
