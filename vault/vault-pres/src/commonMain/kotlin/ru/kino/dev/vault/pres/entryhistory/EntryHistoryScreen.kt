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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.orbitmvi.orbit.compose.collectAsState
import ru.kino.dev.core.pres.*
import ru.kino.dev.ui.components.GradientBackground
import ru.kino.dev.ui.components.SnackbarMessage
import ru.kino.dev.ui.components.skeletonBrush
import ru.kino.dev.ui.res.stringResource
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
    processor: EntryHistoryProcessor = koinViewModel {
        parametersOf(EntryHistoryArgs(dbKey = dbKey, entryId = entryId))
    },
) {
    val state by processor.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    // Every time the screen is shown: a version may have been restored or deleted since
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { processor.onLoad() }

    val message = state.message?.let {
        stringResource(
            when (it) {
                EntryHistoryMessage.LoadFailed -> Res.string.entry_message_load_failed
                EntryHistoryMessage.NotSaved -> Res.string.message_file_not_saved
                EntryHistoryMessage.Unexpected -> Res.string.error_unexpected
            },
        )
    }
    SnackbarMessage(message, snackbar) { processor.onMessageShown() }

    val history = state.history
    GradientBackground {
        Scaffold(
            // The backdrop is the gradient behind
            containerColor = Color.Transparent,
            topBar = {
                // The name of the entry; a skeleton while it is read
                val title = history?.title?.ifEmpty { stringResource(Res.string.entry_untitled) }
                EntryTopBar(
                    title = title?.let { stringResource(Res.string.entry_history_title, it) }.orEmpty(),
                    onBack = processor::onBack,
                    skeleton = if (history == null && state.loading) skeletonBrush() else null,
                    // Only with versions to delete: "Created" is the entry's own and stays
                    menu = if (history?.versions?.isNotEmpty() == true) {
                        listOf(deleteHistoryMenuItem(processor::onDeleteHistory))
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
                    history != null -> historyCards(history, processor)
                    state.loading -> items(SKELETON_ROWS) { SkeletonCard() }
                }
            }
        }

        EntryHistoryDialogs(
            dialog = state.dialog,
            onConfirm = processor::onDialogConfirm,
            onDismiss = processor::onDialogDismiss,
        )
    }
}

/**
 * "Modified" for every old version, newest first - with its menu on a long press - and "Created" last, when
 * it could be read.
 */
private fun LazyListScope.historyCards(history: EntryHistory, actions: EntryHistoryActions) {
    items(history.versions, key = { it.index }) { version ->
        // The reader leaves out a version whose time cannot be read
        version.modified?.let { modified ->
            HistoryCard(
                text = stringResource(Res.string.entry_modified_at, entryTimeText(modified)),
                items = listOf(
                    restoreMenuItem { actions.onRestore(version.index) },
                    deleteVersionMenuItem { actions.onDeleteVersion(version.index) },
                ),
                onOpen = { actions.onOpenVersion(version.index) },
            )
        }
    }
    history.created?.let { created ->
        item(key = CREATED_KEY) {
            HistoryCard(text = stringResource(Res.string.entry_created_at, entryTimeText(created)))
        }
    }
}

private const val SKELETON_ROWS = 4
private const val CREATED_KEY = "created"
