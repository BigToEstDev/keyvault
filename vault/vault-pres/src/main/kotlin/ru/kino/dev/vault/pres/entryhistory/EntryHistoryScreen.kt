package ru.kino.dev.vault.pres.entryhistory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import org.orbitmvi.orbit.compose.collectAsState
import ru.kino.dev.core.pres.R
import ru.kino.dev.ui.components.GradientBackground
import ru.kino.dev.ui.components.SnackbarMessage
import ru.kino.dev.ui.components.skeletonBrush
import ru.kino.dev.vault.history.EntryHistory
import ru.kino.dev.vault.pres.common.EntryTopBar
import ru.kino.dev.vault.pres.common.SkeletonCard
import ru.kino.dev.vault.pres.common.deleteHistoryMenuItem
import ru.kino.dev.vault.pres.common.deleteVersionMenuItem
import ru.kino.dev.vault.pres.common.entryTimeText
import ru.kino.dev.vault.pres.common.restoreMenuItem
import ru.kino.dev.vault.pres.entryhistory.components.EntryHistoryDialogs
import ru.kino.dev.vault.pres.entryhistory.components.HistoryCard

@Composable
internal fun EntryHistoryScreen(
    dbKey: String,
    entryId: String,
    processor: EntryHistoryProcessor = hiltViewModel<EntryHistoryProcessor, EntryHistoryProcessor.Factory>(
        creationCallback = { factory -> factory.create(dbKey, entryId) },
    ),
) {
    val state by processor.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    // Every time the screen is shown: a version may have been restored or deleted since
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { processor.action(EntryHistoryAction.Load) }

    val message = state.message?.let {
        stringResource(
            when (it) {
                EntryHistoryMessage.LoadFailed -> R.string.entry_message_load_failed
                EntryHistoryMessage.NotSaved -> R.string.vault_message_not_saved
                EntryHistoryMessage.Unexpected -> R.string.error_unexpected
            },
        )
    }
    SnackbarMessage(message, snackbar) { processor.action(EntryHistoryAction.MessageShown) }

    val history = state.history
    GradientBackground {
        Scaffold(
            // The backdrop is the gradient behind
            containerColor = Color.Transparent,
            topBar = {
                // The name of the entry; a skeleton while it is read
                val title = history?.title?.ifEmpty { stringResource(R.string.entry_untitled) }
                EntryTopBar(
                    title = title?.let { stringResource(R.string.entry_history_title, it) }.orEmpty(),
                    onBack = { processor.action(EntryHistoryAction.Back) },
                    skeleton = if (history == null && state.loading) skeletonBrush() else null,
                    // Only with versions to delete: "Created" is the entry's own and stays
                    menu = if (history?.versions?.isNotEmpty() == true) {
                        listOf(deleteHistoryMenuItem { processor.action(EntryHistoryAction.DeleteHistory) })
                    } else {
                        emptyList()
                    },
                    menuEnabled = state.dialog?.saving != true,
                )
            },
            snackbarHost = { SnackbarHost(snackbar) },
        ) { padding ->
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when {
                    history != null -> historyCards(history, processor::action)
                    state.loading -> items(SKELETON_ROWS) { SkeletonCard() }
                }
            }
        }

        EntryHistoryDialogs(
            dialog = state.dialog,
            onConfirm = { processor.action(EntryHistoryAction.DialogConfirm) },
            onDismiss = { processor.action(EntryHistoryAction.DialogDismiss) },
        )
    }
}

/**
 * "Modified" for every old version, newest first - with its menu on a long press - and "Created" last, when
 * it could be read.
 */
private fun LazyListScope.historyCards(history: EntryHistory, action: (EntryHistoryAction) -> Unit) {
    items(history.versions, key = { it.index }) { version ->
        // The reader leaves out a version whose time cannot be read
        version.modified?.let { modified ->
            HistoryCard(
                text = stringResource(R.string.entry_modified_at, entryTimeText(modified)),
                items = listOf(
                    restoreMenuItem { action(EntryHistoryAction.Restore(version.index)) },
                    deleteVersionMenuItem { action(EntryHistoryAction.DeleteVersion(version.index)) },
                ),
                onOpen = { action(EntryHistoryAction.OpenVersion(version.index)) },
            )
        }
    }
    history.created?.let { created ->
        item(key = CREATED_KEY) {
            HistoryCard(text = stringResource(R.string.entry_created_at, entryTimeText(created)))
        }
    }
}

private const val SKELETON_ROWS = 4
private const val CREATED_KEY = "created"
