package ru.kino.dev.settings.pres.merge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.orbitmvi.orbit.compose.collectAsState
import ru.kino.dev.core.CredentialLimits
import ru.kino.dev.core.MergeSummary
import ru.kino.dev.core.pres.*
import ru.kino.dev.pane.koinProcessor
import ru.kino.dev.settings.pres.merge.components.CredentialsDialog
import ru.kino.dev.settings.pres.settings.components.SettingsCard
import ru.kino.dev.settings.pres.settings.components.SettingsSectionHeader
import ru.kino.dev.ui.components.BackTopBar
import ru.kino.dev.ui.components.GradientBackground
import ru.kino.dev.ui.components.MessageDialog
import ru.kino.dev.ui.files.DocumentKind
import ru.kino.dev.ui.files.rememberOpenDocument
import ru.kino.dev.vault.sync.OtherDatabaseKind
import ru.kino.dev.vault.sync.counts

/**
 * The page `Синхронизация` of the database (pass-docs, Step 108): "Merge with a copy" and "Import another database" -
 * each picks a `.kdbx`, asks its credentials, merges and writes. The same on a phone and in the container of a wide
 * window - a list of two cards.
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
    val pickImport = rememberOpenDocument(DocumentKind.Database) { processor.onDatabasePicked(OtherDatabaseKind.Import, it) }
    val pickKeyFile = rememberOpenDocument(DocumentKind.KeyFile) { processor.onKeyFilePicked(it) }

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
                item(key = "import") {
                    SettingsCard(
                        icon = Icons.Filled.FileDownload,
                        title = stringResource(Res.string.settings_sync_import),
                        text = stringResource(Res.string.settings_sync_import_text),
                        onClick = pickImport,
                    )
                }
            }
        }
    }

    // What was said comes first: the dialog of the credentials waits under it, to try again
    when (val message = state.message) {
        null -> state.credentials?.let { processor.CredentialsDialog(it, pickKeyFile) }
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
    MergeProblem.Unexpected -> stringResource(Res.string.error_unexpected)
}

private const val MEBIBYTE = 1024L * 1024
