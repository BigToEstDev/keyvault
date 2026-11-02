package ru.kino.dev.vault.pres.trashentry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.orbitmvi.orbit.compose.collectAsState
import ru.kino.dev.core.pres.*
import ru.kino.dev.core.preset.StandardPreset
import ru.kino.dev.ui.components.BackTopBar
import ru.kino.dev.ui.components.GradientBackground
import ru.kino.dev.ui.components.SnackbarMessage
import ru.kino.dev.ui.components.skeletonBrush
import ru.kino.dev.ui.res.stringResource
import ru.kino.dev.vault.pres.common.EntryReadFields
import ru.kino.dev.vault.pres.common.EntryReadSkeleton
import ru.kino.dev.vault.pres.common.EntryTimesText
import ru.kino.dev.vault.pres.trash.components.TrashDialogs

/**
 * An entry of the recycle bin, read: its fields with copying, no group - when it was deleted in its place, under the
 * title (pass-docs, Step 72, item 6); under the fields its times. The bin in the top bar deletes it for good, the fab
 * restores it (pass-docs, Step 71).
 */
@Composable
internal fun TrashEntryScreen(
    dbKey: String,
    entryId: String,
    processor: TrashEntryProcessor = koinViewModel {
        parametersOf(TrashEntryArgs(dbKey = dbKey, entryId = entryId))
    },
) {
    val state by processor.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { processor.onLoad() }

    val message = state.message?.let {
        stringResource(
            when (it) {
                TrashEntryMessage.LoadFailed -> Res.string.entry_message_load_failed
                TrashEntryMessage.Unexpected -> Res.string.error_unexpected
            },
        )
    }
    SnackbarMessage(message, snackbar) { processor.onMessageShown() }

    val entry = state.entry
    val trashed = state.trashed
    // While the entry is read, one brush for the title and the fields - their skeletons run together
    val skeleton = if (state.loading) skeletonBrush() else null

    GradientBackground {
        Scaffold(
            // The backdrop is the gradient behind
            containerColor = Color.Transparent,
            topBar = {
                BackTopBar(
                    title = entry?.content?.value(StandardPreset.TITLE)?.ifEmpty { stringResource(Res.string.entry_untitled) }
                        .orEmpty(),
                    onBack = processor::onBack,
                    skeleton = skeleton,
                ) {
                    if (trashed != null) {
                        IconButton(onClick = processor::onRemove) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(Res.string.trash_remove_title))
                        }
                    }
                }
            },
            floatingActionButton = {
                if (trashed != null) {
                    FloatingActionButton(onClick = processor::onRestore) {
                        Icon(Icons.Filled.RestoreFromTrash, contentDescription = stringResource(Res.string.action_restore))
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbar) },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(CONTENT_PADDING),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when {
                    // No group - restoring asks for one anyway (owner, 23.10.26)
                    entry != null -> {
                        processor.EntryReadFields(
                            content = entry.content,
                            passwordVisible = state.passwordVisible,
                            shownOwn = state.shownOwn,
                            protection = state.protection,
                            shownKeys = state.shownKeys,
                            deletedAt = trashed?.deletedAt,
                        )
                        EntryTimesText(entry.times)
                    }
                    skeleton != null -> EntryReadSkeleton(brush = skeleton)
                }
            }
        }

        TrashDialogs(
            dialog = state.dialog,
            onGroup = processor::onGroupChosen,
            onConfirm = processor::onDialogConfirm,
            onDismiss = processor::onDialogDismiss,
        )

        if (state.notSaved) {
            AlertDialog(
                onDismissRequest = processor::onNotSavedSeen,
                title = { Text(stringResource(Res.string.entry_not_saved_title)) },
                text = { Text(stringResource(Res.string.entry_not_saved_text)) },
                confirmButton = {
                    TextButton(onClick = processor::onNotSavedSeen) {
                        Text(stringResource(Res.string.action_ok))
                    }
                },
            )
        }
    }
}

/** Room around the cards; at the bottom - for the fab, so it never covers the last line. */
private val CONTENT_PADDING = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp)
