package ru.kino.dev.vault.pres.entryread

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import ru.kino.dev.core.pres.*
import ru.kino.dev.core.preset.StandardPreset
import ru.kino.dev.pane.koinProcessor
import ru.kino.dev.ui.components.GradientBackground
import ru.kino.dev.ui.components.SnackbarMessage
import ru.kino.dev.ui.components.deleteMenuItem
import ru.kino.dev.ui.components.skeletonBrush
import ru.kino.dev.ui.files.DocumentKind
import ru.kino.dev.ui.files.rememberCreateDocument
import ru.kino.dev.ui.res.stringResource
import ru.kino.dev.vault.pres.common.EntryDeleteDialog
import ru.kino.dev.vault.pres.common.EntryReadFields
import ru.kino.dev.vault.pres.common.EntryReadSkeleton
import ru.kino.dev.vault.pres.common.EntryTimesText
import ru.kino.dev.vault.pres.common.EntryTopBar
import ru.kino.dev.vault.pres.common.LockFab
import ru.kino.dev.vault.pres.common.historyMenuItem

/** @param canGoBack false - a pane of the container of a wide window: its tab closes it, not "back" (pass-docs, Step 98) */
@Composable
internal fun EntryReadScreen(
    dbKey: String,
    entryId: String,
    canGoBack: Boolean = true,
    processor: EntryReadProcessor = koinProcessor(EntryReadArgs(dbKey = dbKey, entryId = entryId)),
) {
    val state by processor.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    // Every time the screen is shown: back from the form, the entry may differ
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { processor.onLoad() }

    // "Download": the file to save the attachment as, under its name (pass-docs, Step 103)
    val saveAttachment = rememberCreateDocument(DocumentKind.Attachment) { processor.onDownloadPicked(it) }
    processor.collectSideEffect { effect ->
        when (effect) {
            is EntryReadSideEffect.SaveAttachment -> saveAttachment(effect.name)
        }
    }

    val message = state.message?.let {
        stringResource(
            when (it) {
                EntryReadMessage.LoadFailed -> Res.string.entry_message_load_failed
                EntryReadMessage.NotSaved -> Res.string.message_file_not_saved
                EntryReadMessage.Unexpected -> Res.string.error_unexpected
                EntryReadMessage.AttachmentSaved -> Res.string.entry_attachment_saved
                EntryReadMessage.AttachmentNotSaved -> Res.string.entry_attachment_not_saved
            },
        )
    }
    SnackbarMessage(message, snackbar) { processor.onMessageShown() }

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
                    title = title?.ifEmpty { stringResource(Res.string.entry_untitled) }.orEmpty(),
                    onBack = processor::onBack,
                    back = canGoBack,
                    skeleton = skeleton,
                    // Once the entry is read: there is nothing to delete before
                    menu = if (state.content != null) {
                        listOf(
                            historyMenuItem(processor::onHistory),
                            deleteMenuItem(processor::onDelete),
                        )
                    } else {
                        emptyList()
                    },
                )
            },
            floatingActionButton = {
                if (state.content != null) {
                    FloatingActionButton(onClick = processor::onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(Res.string.entry_edit))
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
                        processor.EntryReadFields(
                            content = content,
                            passwordVisible = state.passwordVisible,
                            shownOwn = state.shownOwn,
                            protection = state.protection,
                            shownKeys = state.shownKeys,
                            group = state.group,
                            attachments = state.attachments,
                            downloading = state.downloading,
                        )
                        EntryTimesText(state.times)
                    }
                    skeleton != null -> EntryReadSkeleton(brush = skeleton)
                }
            }
        }

        // Out of the scaffold's slot, which "edit" holds - in the other corner (pass-docs, Step 59)
        LockFab(
            locking = state.locking,
            onLock = processor::onLock,
            modifier = Modifier.align(Alignment.BottomStart),
        )

        state.delete?.let { dialog ->
            EntryDeleteDialog(
                title = dialog.title,
                toBin = dialog.toBin,
                deleting = dialog.deleting,
                onConfirm = processor::onDeleteConfirm,
                onDismiss = processor::onDeleteDismiss,
            )
        }
    }
}

/** Room around the cards; at the bottom - for the fab, so it never covers the last field. */
private val CONTENT_PADDING = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp)
