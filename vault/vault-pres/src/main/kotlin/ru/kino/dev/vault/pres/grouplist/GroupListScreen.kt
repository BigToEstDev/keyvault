package ru.kino.dev.vault.pres.grouplist

import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.compose.collectAsState
import ru.kino.dev.core.pres.R
import ru.kino.dev.ui.components.EmptyState
import ru.kino.dev.ui.components.ExpandableFab
import ru.kino.dev.ui.components.FabItem
import ru.kino.dev.ui.components.GradientBackground
import ru.kino.dev.vault.pres.grouplist.components.DebugLockFab
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

    BackHandler(enabled = state.fabExpanded) { processor.action(GroupListAction.ToggleFab) }

    Messages(state.message, snackbar) { processor.action(GroupListAction.MessageShown) }

    GradientBackground {
        Scaffold(
            // The backdrop is the gradient behind
            containerColor = Color.Transparent,
            topBar = {
                GroupListTopBar(
                    path = state.path,
                    entryCount = state.entryCount,
                    query = state.query,
                    onQueryChange = { processor.action(GroupListAction.QueryChanged(it)) },
                    onBack = { processor.action(GroupListAction.Back) },
                    onCrumb = { processor.action(GroupListAction.OpenCrumb(it)) },
                )
            },
            snackbarHost = { SnackbarHost(snackbar) },
            floatingActionButton = {
                ExpandableFab(
                    expanded = state.fabExpanded,
                    onToggle = { processor.action(GroupListAction.ToggleFab) },
                    items = listOf(
                        FabItem(Icons.Filled.CreateNewFolder, stringResource(R.string.vault_create_group)) {
                            processor.action(GroupListAction.CreateGroup)
                        },
                        FabItem(Icons.Filled.Key, stringResource(R.string.vault_create_entry)) {
                            processor.action(GroupListAction.CreateEntry)
                        },
                    ),
                )
            },
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
                .padding(16.dp),
        )

        GroupDialogs(
            dialog = state.dialog,
            onNameChange = { processor.action(GroupListAction.DialogNameChanged(it)) },
            onConfirm = { processor.action(GroupListAction.DialogConfirm) },
            onDismiss = { processor.action(GroupListAction.DialogDismiss) },
        )
    }
}

@Composable
private fun GroupListContent(state: GroupListState, padding: PaddingValues, action: (GroupListAction) -> Unit) {
    when {
        state.loading -> LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            userScrollEnabled = false,
        ) {
            items(SKELETON_ROWS) { SkeletonCard() }
        }

        state.groups.isEmpty() -> Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = EMPTY_ALIGNMENT,
        ) {
            EmptyState(
                title = stringResource(R.string.vault_group_empty),
                hint = stringResource(R.string.vault_group_empty_hint),
            )
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
                    onOpen = { action(GroupListAction.OpenGroup(group.id)) },
                    onEdit = { action(GroupListAction.RenameGroup(group.id)) },
                    onDelete = { action(GroupListAction.DeleteGroup(group.id)) },
                )
            }
        }
    }
}

/**
 * Hands the message to the snackbar and tells the processor it is taken at once - the snackbar shows it on
 * a scope of its own, so clearing the message does not cut it short.
 */
@Composable
private fun Messages(message: GroupListMessage?, snackbar: SnackbarHostState, onShown: () -> Unit) {
    val scope = rememberCoroutineScope()
    val text = message?.let { stringResource(it.text) }

    LaunchedEffect(message) {
        if (text != null) {
            scope.launch { snackbar.showSnackbar(text) }
            onShown()
        }
    }
}

private val GroupListMessage.text: Int
    get() = when (this) {
        GroupListMessage.NotSaved -> R.string.vault_message_not_saved
        GroupListMessage.LoadFailed -> R.string.vault_message_load_failed
        GroupListMessage.Unexpected -> R.string.error_unexpected
    }

/** The empty state a little above the middle: the eye lands there, and the buttons take the bottom. */
private val EMPTY_ALIGNMENT = BiasAlignment(horizontalBias = 0f, verticalBias = -0.3f)

/** Cards drawn while the tree is read - about a screenful. */
private const val SKELETON_ROWS = 6

private val FAB_CLEARANCE = 96.dp
