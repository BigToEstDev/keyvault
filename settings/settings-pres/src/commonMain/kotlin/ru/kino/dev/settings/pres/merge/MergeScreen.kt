package ru.kino.dev.settings.pres.merge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import ru.kino.dev.core.CredentialLimits
import ru.kino.dev.core.MergeSummary
import ru.kino.dev.core.pres.*
import ru.kino.dev.pane.koinProcessor
import ru.kino.dev.settings.pres.merge.components.CredentialsDialog
import ru.kino.dev.settings.pres.settings.components.SettingsCard
import ru.kino.dev.settings.pres.settings.components.SettingsSectionHeader
import ru.kino.dev.ui.components.BackTopBar
import ru.kino.dev.ui.components.GradientBackground
import ru.kino.dev.ui.components.LoadingDialog
import ru.kino.dev.ui.components.MessageDialog
import ru.kino.dev.ui.files.DocumentKind
import ru.kino.dev.ui.files.rememberCreateDocument
import ru.kino.dev.ui.files.rememberOpenDocument
import ru.kino.dev.ui.files.rememberShareFile
import ru.kino.dev.ui.res.stringResource
import ru.kino.dev.vault.sync.OtherDatabaseKind
import ru.kino.dev.vault.sync.counts

/**
 * The page `Синхронизация` of the database (pass-docs, Step 108, 110): "Merge with a copy", "Synchronize with a copy"
 * (both ways - the copy's file written too) and "Import another database" - each picks a `.kdbx`, asks its
 * credentials, merges and writes. Below them "Save a copy" and "Send" - android only - the file of the database as it is
 * (Step 111). The same on a phone and in the container of a wide window - a list of cards.
 *
 * @param canGoBack false - a page of the container of a wide window: its tab closes it, not "back" (Step 101)
 */
@Composable
internal fun MergeScreen(
    dbKey: String,
    canGoBack: Boolean = true,
    processor: MergeProcessor = koinProcessor(dbKey),
) {
    val state by processor.collectAsState()

    val pickCopy = rememberOpenDocument(DocumentKind.Database) { processor.onDatabasePicked(OtherDatabaseKind.Copy, it) }
    val pickBothWays =
        rememberOpenDocument(DocumentKind.Database) { processor.onDatabasePicked(OtherDatabaseKind.CopyBothWays, it) }
    val pickImport = rememberOpenDocument(DocumentKind.Database) { processor.onDatabasePicked(OtherDatabaseKind.Import, it) }
    val pickKeyFile = rememberOpenDocument(DocumentKind.KeyFile) { processor.onKeyFilePicked(it) }
    val pickCopyTarget = rememberCreateDocument(DocumentKind.Database) { processor.onCopyTargetPicked(it) }
    val share = rememberShareFile()
    processor.collectSideEffect { effect ->
        when (effect) {
            is MergeSideEffect.PickCopyTarget -> pickCopyTarget(effect.name)
            is MergeSideEffect.Share -> share(effect.uri)
        }
    }

    GradientBackground {
        Scaffold(
            // The backdrop is the gradient behind
            containerColor = Color.Transparent,
            topBar = {
                BackTopBar(title = stringResource(Res.string.settings_sync), onBack = processor::onBack, back = canGoBack)
            },
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "header") {
                    SettingsSectionHeader(
                        title = stringResource(Res.string.settings_sync),
                        hint = stringResource(Res.string.settings_sync_hint),
                    )
                }
                item(key = "merge") {
                    SettingsCard(
                        icon = Icons.Filled.SyncAlt,
                        title = stringResource(Res.string.settings_sync_merge),
                        text = stringResource(Res.string.settings_sync_merge_text),
                        onClick = pickCopy,
                    )
                }
                item(key = "both-ways") {
                    SettingsCard(
                        icon = Icons.Filled.Sync,
                        title = stringResource(Res.string.settings_sync_both_ways),
                        text = stringResource(Res.string.settings_sync_both_ways_text),
                        onClick = pickBothWays,
                    )
                }
                item(key = "import") {
                    SettingsCard(
                        icon = Icons.Filled.FileDownload,
                        title = stringResource(Res.string.settings_sync_import),
                        text = stringResource(Res.string.settings_sync_import_text),
                        onClick = pickImport,
                    )
                }
                item(key = "save-copy") {
                    SettingsCard(
                        icon = Icons.Filled.SaveAlt,
                        title = stringResource(Res.string.settings_sync_save_copy),
                        text = stringResource(Res.string.settings_sync_save_copy_text),
                        onClick = processor::onSaveCopy,
                    )
                }
                if (state.shareAvailable) {
                    item(key = "share") {
                        SettingsCard(
                            icon = Icons.Filled.Share,
                            title = stringResource(Res.string.settings_sync_share),
                            text = stringResource(Res.string.settings_sync_share_text),
                            onClick = processor::onShare,
                        )
                    }
                }
            }
        }
    }

    if (state.copying) LoadingDialog(stringResource(Res.string.settings_sync_copying))

    // What was said comes first: the dialog of the credentials waits under it, to try again
    when (val message = state.message) {
        null -> state.credentials?.let { dialog ->
            val pickDatabase = when (dialog.kind) {
                OtherDatabaseKind.Copy -> pickCopy
                OtherDatabaseKind.CopyBothWays -> pickBothWays
                OtherDatabaseKind.Import -> pickImport
            }
            processor.CredentialsDialog(dialog, pickDatabase = pickDatabase, pickKeyFile = pickKeyFile)
        }
        is MergeMessage.Done -> MessageDialog(
            title = stringResource(Res.string.sync_done_title),
            text = doneText(message.summary),
            onDismiss = processor::onDismissMessage,
        )
        is MergeMessage.Failed -> MessageDialog(
            title = stringResource(Res.string.sync_failed_title),
            text = failedText(message.problem),
            onDismiss = processor::onDismissMessage,
        )
        MergeMessage.CopySaved -> MessageDialog(
            title = stringResource(Res.string.settings_sync_copy_saved_title),
            text = stringResource(Res.string.settings_sync_copy_saved_text),
            onDismiss = processor::onDismissMessage,
        )
        is MergeMessage.CopyFailed -> MessageDialog(
            title = stringResource(Res.string.settings_sync_copy_failed_title),
            text = copyFailedText(message.problem),
            onDismiss = processor::onDismissMessage,
        )
    }
}

@Composable
private fun doneText(summary: MergeSummary): String {
    val counts = summary.counts
    return if (counts.none) {
        stringResource(Res.string.sync_done_settings)
    } else {
        stringResource(Res.string.sync_done_text, counts.added, counts.changed, counts.deleted)
    }
}

@Composable
private fun failedText(problem: MergeProblem): String = when (problem) {
    MergeProblem.NotSameDatabase -> stringResource(Res.string.settings_sync_failed_not_same)
    MergeProblem.SameDatabase -> stringResource(Res.string.settings_sync_failed_same)
    MergeProblem.Damaged -> stringResource(Res.string.sync_failed_damaged)
    MergeProblem.Unreachable -> stringResource(Res.string.error_database_unreachable)
    MergeProblem.KeyFileTooLarge ->
        stringResource(Res.string.error_key_file_too_large, (CredentialLimits.KEY_FILE_MAX_SIZE / MEBIBYTE).toInt())
    MergeProblem.KeyFileUnreachable -> stringResource(Res.string.error_key_file_unreachable)
    MergeProblem.NotSaved -> stringResource(Res.string.sync_failed_not_saved)
    MergeProblem.OwnFile -> stringResource(Res.string.settings_sync_failed_own_file)
    MergeProblem.CopyNotWritten -> stringResource(Res.string.settings_sync_failed_copy_not_written)
    MergeProblem.CopyNotWrittenChangedElsewhere ->
        stringResource(Res.string.settings_sync_failed_copy_changed_elsewhere)
    MergeProblem.Unexpected -> stringResource(Res.string.error_unexpected)
}

@Composable
private fun copyFailedText(problem: CopyProblem): String = when (problem) {
    CopyProblem.OwnFile -> stringResource(Res.string.settings_sync_failed_own_file)
    CopyProblem.NotSaved -> stringResource(Res.string.settings_sync_copy_failed_not_saved)
    CopyProblem.ChangedElsewhere -> stringResource(Res.string.settings_sync_copy_failed_changed_elsewhere)
    CopyProblem.Unreadable -> stringResource(Res.string.error_database_unreachable)
    CopyProblem.NotWritten -> stringResource(Res.string.settings_sync_copy_failed_not_written)
}

private const val MEBIBYTE = 1024L * 1024
