package ru.kino.dev.vault.pres.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.orbitmvi.orbit.compose.collectAsState
import ru.kino.dev.core.MergeSummary
import ru.kino.dev.core.pres.Res
import ru.kino.dev.core.pres.action_cancel
import ru.kino.dev.core.pres.action_later
import ru.kino.dev.core.pres.action_save
import ru.kino.dev.core.pres.action_synchronize
import ru.kino.dev.core.pres.error_unexpected
import ru.kino.dev.core.pres.save_damaged_text
import ru.kino.dev.core.pres.save_damaged_title
import ru.kino.dev.core.pres.save_saving
import ru.kino.dev.core.pres.sync_ask_text
import ru.kino.dev.core.pres.sync_ask_title
import ru.kino.dev.core.pres.sync_done_settings
import ru.kino.dev.core.pres.sync_done_text
import ru.kino.dev.core.pres.sync_done_title
import ru.kino.dev.core.pres.sync_failed_credentials
import ru.kino.dev.core.pres.sync_failed_damaged
import ru.kino.dev.core.pres.sync_failed_not_same
import ru.kino.dev.core.pres.sync_failed_not_saved
import ru.kino.dev.core.pres.sync_failed_title
import ru.kino.dev.core.pres.sync_failed_unreachable
import ru.kino.dev.core.pres.sync_syncing
import ru.kino.dev.ui.components.ConfirmDialog
import ru.kino.dev.ui.components.MessageDialog
import ru.kino.dev.ui.res.stringResource
import ru.kino.dev.vault.sync.counts

/**
 * The sync of the open database over the screens of `vault` (pass-docs, Step 108): its dialogs and, while a merge is
 * under way, a quiet sign on top - the screens under it work on.
 *
 * Put by the app over `vault` for as long as it is shown; the processor is of the graph of `vault` - the caller gives
 * that owner - so it lives from opening or unlocking to locking.
 */
@Composable
fun VaultSyncHost(dbKey: String) {
    val processor: SyncProcessor = koinViewModel { parametersOf(dbKey) }
    val state by processor.collectAsState()

    processor.SyncContent(state)
}

@Composable
private fun SyncActions.SyncContent(state: SyncState) {
    // Saving from the dialog shows on its button; from the bar of the main screen - here (pass-docs, Step 112)
    val sign = when {
        state.syncing -> Res.string.sync_syncing
        state.saving && state.dialog != SyncDialog.Damaged -> Res.string.save_saving
        else -> null
    }
    if (sign != null) {
        Box(modifier = Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
            SyncingSign(text = stringResource(sign), modifier = Modifier.padding(top = 8.dp))
        }
    }

    when (val dialog = state.dialog) {
        null -> Unit
        SyncDialog.Ask -> ConfirmDialog(
            title = stringResource(Res.string.sync_ask_title),
            text = stringResource(Res.string.sync_ask_text),
            confirmText = stringResource(Res.string.action_synchronize),
            dismissText = stringResource(Res.string.action_cancel),
            onConfirm = ::onSynchronize,
            onDismiss = ::onCancel,
        )
        is SyncDialog.Done -> MessageDialog(
            title = stringResource(Res.string.sync_done_title),
            text = doneText(dialog.summary),
            onDismiss = ::onDismiss,
        )
        is SyncDialog.Failed -> MessageDialog(
            title = stringResource(Res.string.sync_failed_title),
            text = stringResource(dialog.problem.text),
            onDismiss = ::onDismiss,
        )
        // Only its buttons close it: closed by a tap past it, the database may be closed before it is written
        SyncDialog.Damaged -> ConfirmDialog(
            title = stringResource(Res.string.save_damaged_title),
            text = stringResource(Res.string.save_damaged_text),
            confirmText = stringResource(Res.string.action_save),
            dismissText = stringResource(Res.string.action_later),
            onConfirm = ::onSave,
            onDismiss = ::onLater,
            loading = state.saving,
            dismissOnOutside = false,
        )
    }
}

@Composable
private fun SyncingSign(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** What came in, as a person sees it ([counts]); only the settings of the database - said so. */
@Composable
private fun doneText(summary: MergeSummary): String {
    val counts = summary.counts
    return if (counts.none) {
        stringResource(Res.string.sync_done_settings)
    } else {
        stringResource(Res.string.sync_done_text, counts.added, counts.changed, counts.deleted)
    }
}

private val SyncProblem.text: StringResource
    get() = when (this) {
        SyncProblem.CredentialsChanged -> Res.string.sync_failed_credentials
        SyncProblem.NotSameDatabase -> Res.string.sync_failed_not_same
        SyncProblem.Damaged -> Res.string.sync_failed_damaged
        SyncProblem.Unreachable -> Res.string.sync_failed_unreachable
        SyncProblem.NotSaved -> Res.string.sync_failed_not_saved
        SyncProblem.Unexpected -> Res.string.error_unexpected
    }
