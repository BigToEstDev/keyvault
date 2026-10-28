package ru.kino.dev.vault.pres.trash

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import org.jetbrains.compose.resources.StringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.orbitmvi.orbit.compose.collectAsState
import ru.kino.dev.core.pres.*
import ru.kino.dev.ui.components.BackTopBar
import ru.kino.dev.ui.components.EmptySearchIllustration
import ru.kino.dev.ui.components.EmptyState
import ru.kino.dev.ui.components.GradientBackground
import ru.kino.dev.ui.components.MenuItem
import ru.kino.dev.ui.components.MoreMenuButton
import ru.kino.dev.ui.components.PrimaryButton
import ru.kino.dev.ui.components.SearchBar
import ru.kino.dev.ui.components.SecondaryButton
import ru.kino.dev.ui.components.SnackbarMessage
import ru.kino.dev.ui.res.stringResource
import ru.kino.dev.vault.pres.common.SkeletonCard
import ru.kino.dev.vault.pres.trash.components.TrashCard
import ru.kino.dev.vault.pres.trash.components.TrashDialogs

/**
 * The recycle bin: its entries, the last deleted first, and the search over them; a long press picks, the picked
 * are restored or deleted for good from the buttons at the bottom; the menu empties the whole bin (pass-docs,
 * Step 70).
 */
@Composable
internal fun TrashScreen(
    dbKey: String,
    processor: TrashProcessor = koinViewModel { parametersOf(dbKey) },
) {
    val state by processor.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    // Every time the screen is shown: after unlocking, the bin may differ
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { processor.action(TrashAction.Load) }

    // "Back" lets go of the choice first
    BackHandler(enabled = state.choosing) { processor.action(TrashAction.Back) }

    SnackbarMessage(state.message?.let { stringResource(it.text) }, snackbar) {
        processor.action(TrashAction.MessageShown)
    }

    GradientBackground {
        Scaffold(
            // The backdrop is the gradient behind
            containerColor = Color.Transparent,
            topBar = {
                Column {
                    if (state.choosing) {
                        ChoiceTopBar(count = state.picked.size, action = processor::action)
                    } else {
                        BackTopBar(
                            title = stringResource(Res.string.settings_recycle_bin),
                            onBack = { processor.action(TrashAction.Back) },
                            actions = {
                                MoreMenuButton(
                                    contentDescription = stringResource(Res.string.trash_menu),
                                    items = listOf(
                                        MenuItem(stringResource(Res.string.trash_empty_action), Icons.Filled.DeleteForever) {
                                            processor.action(TrashAction.Empty)
                                        },
                                    ),
                                    enabled = state.entries.orEmpty().isNotEmpty(),
                                )
                            },
                        )
                    }
                    SearchBar(
                        query = state.query,
                        onQueryChange = { processor.action(TrashAction.QueryChanged(it)) },
                        placeholder = stringResource(Res.string.vault_search),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                    )
                }
            },
            bottomBar = {
                if (state.choosing) ChoiceButtons(processor::action)
            },
            snackbarHost = { SnackbarHost(snackbar) },
        ) { padding ->
            TrashContent(state, padding, processor::action)
        }

        TrashDialogs(
            dialog = state.dialog,
            onGroup = { processor.action(TrashAction.GroupChosen(it)) },
            onConfirm = { processor.action(TrashAction.DialogConfirm) },
            onDismiss = { processor.action(TrashAction.DialogDismiss) },
        )
    }
}

/** The top bar while choosing: the cross lets go of everything, how many are picked, "pick all". */
@Composable
private fun ChoiceTopBar(count: Int, action: (TrashAction) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { action(TrashAction.ClearChoice) }) {
            Icon(Icons.Filled.Close, contentDescription = stringResource(Res.string.action_close))
        }
        Text(
            text = stringResource(Res.string.trash_picked, count),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
        )
        IconButton(onClick = { action(TrashAction.PickAll) }) {
            Icon(Icons.Filled.SelectAll, contentDescription = stringResource(Res.string.action_select_all))
        }
    }
}

/** Under the list while choosing: restore the picked, or delete them for good. */
@Composable
private fun ChoiceButtons(action: (TrashAction) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SecondaryButton(
            text = stringResource(Res.string.action_delete),
            onClick = { action(TrashAction.Remove) },
            modifier = Modifier.weight(1f),
        )
        PrimaryButton(
            text = stringResource(Res.string.action_restore),
            onClick = { action(TrashAction.Restore) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TrashContent(state: TrashState, padding: PaddingValues, action: (TrashAction) -> Unit) {
    val empty = state.empty
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
            TrashEmptyState(empty, state.query)
        }

        else -> LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.shown, key = { it.entry.id.value }) { item ->
                val id = item.entry.id
                TrashCard(
                    item = item,
                    choosing = state.choosing,
                    picked = id in state.picked,
                    onTap = { action(TrashAction.Tap(id)) },
                    onLongPress = { action(TrashAction.LongPress(id)) },
                )
            }
        }
    }
}

@Composable
private fun TrashEmptyState(reason: TrashEmpty, query: String) {
    when (reason) {
        TrashEmpty.Bin -> EmptyState(
            title = stringResource(Res.string.trash_empty),
            hint = stringResource(Res.string.trash_empty_hint),
        )

        TrashEmpty.NothingFound -> EmptyState(
            title = stringResource(Res.string.vault_search_nothing_found, query.trim()),
            hint = stringResource(Res.string.vault_search_hint_fields) + "\n" +
                stringResource(Res.string.vault_search_check_spelling),
            illustration = { EmptySearchIllustration() },
        )
    }
}

private val TrashMessage.text: StringResource
    get() = when (this) {
        TrashMessage.LoadFailed -> Res.string.vault_message_load_failed
        TrashMessage.SearchFailed -> Res.string.vault_message_search_failed
        TrashMessage.NotSaved -> Res.string.message_file_not_saved
        TrashMessage.Changed -> Res.string.trash_message_changed
        TrashMessage.Unexpected -> Res.string.error_unexpected
    }

/** The empty state a little above the middle, as in the list of `vault`. */
private val EMPTY_ALIGNMENT = BiasAlignment(horizontalBias = 0f, verticalBias = -0.3f)

/** Cards drawn while the bin is read - about a screenful. */
private const val SKELETON_ROWS = 6
