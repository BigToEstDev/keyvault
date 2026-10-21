package ru.kino.dev.vault.pres.entryread

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import org.orbitmvi.orbit.compose.collectAsState
import ru.kino.dev.core.pres.R
import ru.kino.dev.core.preset.StandardPreset
import ru.kino.dev.ui.components.GradientBackground
import ru.kino.dev.ui.components.SnackbarMessage
import ru.kino.dev.ui.components.deleteMenuItem
import ru.kino.dev.ui.components.skeletonBrush
import ru.kino.dev.vault.entry.EntryTimes
import ru.kino.dev.vault.pres.common.EntryDeleteDialog
import ru.kino.dev.vault.pres.common.EntryReadFields
import ru.kino.dev.vault.pres.common.EntrySkeleton
import ru.kino.dev.vault.pres.common.EntryTopBar
import ru.kino.dev.vault.pres.common.LockFab
import ru.kino.dev.vault.pres.common.entryTimeText
import ru.kino.dev.vault.pres.common.historyMenuItem

@Composable
internal fun EntryReadScreen(
    dbKey: String,
    entryId: String,
    processor: EntryReadProcessor = hiltViewModel<EntryReadProcessor, EntryReadProcessor.Factory>(
        creationCallback = { factory -> factory.create(dbKey, entryId) },
    ),
) {
    val state by processor.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    // Every time the screen is shown: back from the form, the entry may differ
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { processor.action(EntryReadAction.Load) }

    val message = state.message?.let {
        stringResource(
            when (it) {
                EntryReadMessage.LoadFailed -> R.string.entry_message_load_failed
                EntryReadMessage.NotSaved -> R.string.message_file_not_saved
                EntryReadMessage.Unexpected -> R.string.error_unexpected
            },
        )
    }
    SnackbarMessage(message, snackbar) { processor.action(EntryReadAction.MessageShown) }

    // While the entry is read, one brush for the title and the fields - their skeletons run together
    val skeleton = if (state.content == null && state.loading) skeletonBrush() else null

    GradientBackground {
        Scaffold(
            // The backdrop is the gradient behind
            containerColor = Color.Transparent,
            topBar = {
                // The title of the entry; a skeleton while it is read, nothing if it could not be
                val title = state.content?.value(StandardPreset.TITLE)
                EntryTopBar(
                    title = title?.ifEmpty { stringResource(R.string.entry_untitled) }.orEmpty(),
                    onBack = { processor.action(EntryReadAction.Back) },
                    skeleton = skeleton,
                    // Once the entry is read: there is nothing to delete before
                    menu = if (state.content != null) {
                        listOf(
                            historyMenuItem { processor.action(EntryReadAction.History) },
                            deleteMenuItem { processor.action(EntryReadAction.Delete) },
                        )
                    } else {
                        emptyList()
                    },
                )
            },
            floatingActionButton = {
                if (state.content != null) {
                    FloatingActionButton(onClick = { processor.action(EntryReadAction.Edit) }) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.entry_edit))
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
                val content = state.content
                when {
                    content != null -> {
                        EntryReadFields(
                            content = content,
                            passwordVisible = state.passwordVisible,
                            shownOwn = state.shownOwn,
                            onTogglePassword = { processor.action(EntryReadAction.TogglePassword) },
                            onToggleOwn = { processor.action(EntryReadAction.ToggleOwn(it)) },
                            group = state.group,
                        )
                        EntryTimesText(state.times)
                    }
                    skeleton != null -> EntrySkeleton(brush = skeleton)
                }
            }
        }

        // Out of the scaffold's slot, which "edit" holds - in the other corner (pass-docs, Step 59)
        LockFab(
            locking = state.locking,
            onLock = { processor.action(EntryReadAction.Lock) },
            modifier = Modifier.align(Alignment.BottomStart),
        )

        state.delete?.let { dialog ->
            EntryDeleteDialog(
                title = dialog.title,
                deleting = dialog.deleting,
                onConfirm = { processor.action(EntryReadAction.DeleteConfirm) },
                onDismiss = { processor.action(EntryReadAction.DeleteDismiss) },
            )
        }
    }
}

/**
 * Under the cards, in the middle: when the entry was last changed, and under it when it was made (pass-docs,
 * Step 51, item 2). A time that could not be read has no line.
 */
@Composable
private fun EntryTimesText(times: EntryTimes) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        times.modified?.let { TimeLine(stringResource(R.string.entry_modified_at, entryTimeText(it))) }
        times.created?.let { TimeLine(stringResource(R.string.entry_created_at, entryTimeText(it))) }
    }
}

@Composable
private fun TimeLine(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** Room around the cards; at the bottom - for the fab, so it never covers the last field. */
private val CONTENT_PADDING = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp)
