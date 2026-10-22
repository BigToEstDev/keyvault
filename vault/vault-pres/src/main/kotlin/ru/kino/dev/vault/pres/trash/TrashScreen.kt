package ru.kino.dev.vault.pres.trash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import ru.kino.dev.ui.components.BackTopBar
import ru.kino.dev.ui.components.EmptySearchIllustration
import ru.kino.dev.ui.components.EmptyState
import ru.kino.dev.ui.components.GradientBackground
import ru.kino.dev.ui.components.SearchBar
import ru.kino.dev.ui.components.SnackbarMessage
import ru.kino.dev.vault.pres.common.SkeletonCard
import ru.kino.dev.vault.pres.trash.components.TrashCard

/** The recycle bin: its entries, the last deleted first, and the search over them (pass-docs, Step 70). */
@Composable
internal fun TrashScreen(
    dbKey: String,
    processor: TrashProcessor = hiltViewModel<TrashProcessor, TrashProcessor.Factory>(
        creationCallback = { factory -> factory.create(dbKey) },
    ),
) {
    val state by processor.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    // Every time the screen is shown: after unlocking, the bin may differ
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { processor.action(TrashAction.Load) }

    val message = state.message?.let {
        stringResource(
            when (it) {
                TrashMessage.LoadFailed -> R.string.vault_message_load_failed
                TrashMessage.SearchFailed -> R.string.vault_message_search_failed
            },
        )
    }
    SnackbarMessage(message, snackbar) { processor.action(TrashAction.MessageShown) }

    GradientBackground {
        Scaffold(
            // The backdrop is the gradient behind
            containerColor = Color.Transparent,
            topBar = {
                Column {
                    BackTopBar(
                        title = stringResource(R.string.settings_recycle_bin),
                        onBack = { processor.action(TrashAction.Back) },
                    )
                    SearchBar(
                        query = state.query,
                        onQueryChange = { processor.action(TrashAction.QueryChanged(it)) },
                        placeholder = stringResource(R.string.vault_search),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbar) },
        ) { padding ->
            TrashContent(state, padding)
        }
    }
}

@Composable
private fun TrashContent(state: TrashState, padding: PaddingValues) {
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
            items(state.shown, key = { it.entry.id.value }) { item -> TrashCard(item) }
        }
    }
}

@Composable
private fun TrashEmptyState(reason: TrashEmpty, query: String) {
    when (reason) {
        TrashEmpty.Bin -> EmptyState(
            title = stringResource(R.string.trash_empty),
            hint = stringResource(R.string.trash_empty_hint),
        )

        TrashEmpty.NothingFound -> EmptyState(
            title = stringResource(R.string.vault_search_nothing_found, query.trim()),
            hint = stringResource(R.string.vault_search_hint_fields) + "\n" +
                stringResource(R.string.vault_search_check_spelling),
            illustration = { EmptySearchIllustration() },
        )
    }
}

/** The empty state a little above the middle, as in the list of `vault`. */
private val EMPTY_ALIGNMENT = BiasAlignment(horizontalBias = 0f, verticalBias = -0.3f)

/** Cards drawn while the bin is read - about a screenful. */
private const val SKELETON_ROWS = 6
