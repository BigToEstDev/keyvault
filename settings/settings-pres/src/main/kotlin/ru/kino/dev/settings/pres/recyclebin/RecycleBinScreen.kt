package ru.kino.dev.settings.pres.recyclebin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
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
import ru.kino.dev.settings.pres.recyclebin.components.RecycleBinSkeleton
import ru.kino.dev.ui.components.BackTopBar
import ru.kino.dev.ui.components.ConfirmDialog
import ru.kino.dev.ui.components.GradientBackground
import ru.kino.dev.ui.components.SnackbarMessage
import ru.kino.dev.ui.components.SwitchRow
import ru.kino.dev.ui.components.ValueRow
import ru.kino.dev.ui.components.skeletonBrush
import ru.kino.dev.ui.res.stringResource

/** The recycle bin of the database: whether it is used, and what it holds (pass-docs, Step 70). */
@Composable
internal fun RecycleBinScreen(
    dbKey: String,
    processor: RecycleBinProcessor = koinViewModel { parametersOf(dbKey) },
) {
    val state by processor.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    // Every time the screen is shown: back from the bin, it may hold less
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { processor.action(RecycleBinAction.Load) }

    val message = state.message?.let {
        stringResource(
            when (it) {
                RecycleBinMessage.NotSaved -> Res.string.message_file_not_saved
                RecycleBinMessage.Failed -> Res.string.error_unexpected
            },
        )
    }
    SnackbarMessage(message, snackbar) { processor.action(RecycleBinAction.MessageShown) }

    GradientBackground {
        Scaffold(
            // The backdrop is the gradient behind
            containerColor = Color.Transparent,
            topBar = {
                BackTopBar(
                    title = stringResource(Res.string.settings_recycle_bin),
                    onBack = { processor.action(RecycleBinAction.Back) },
                )
            },
            snackbarHost = { SnackbarHost(snackbar) },
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val enabled = state.enabled
                if (enabled == null) {
                    // Until read there is nothing to show for real: the shapes of the screen
                    item(key = "skeleton") { RecycleBinSkeleton(skeletonBrush()) }
                } else {
                    item(key = "use") { UseCard(enabled, state.saving, processor::action) }
                    item(key = "deleted") {
                        DeletedCard(state.count, state.saving) { processor.action(RecycleBinAction.OpenTrash) }
                    }
                }
            }
        }
    }

    if (state.confirmingOff) {
        ConfirmDialog(
            title = stringResource(Res.string.settings_recycle_bin_off_title),
            text = stringResource(Res.string.settings_recycle_bin_off_text),
            confirmText = stringResource(Res.string.settings_recycle_bin_off_confirm),
            dismissText = stringResource(Res.string.action_cancel),
            onConfirm = { processor.action(RecycleBinAction.OffConfirm) },
            onDismiss = { processor.action(RecycleBinAction.OffDismiss) },
            destructive = true,
        )
    }
}

@Composable
private fun UseCard(enabled: Boolean, saving: Boolean, action: (RecycleBinAction) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            SwitchRow(
                title = stringResource(Res.string.settings_recycle_bin_use),
                text = stringResource(
                    if (enabled) Res.string.settings_recycle_bin_use_on else Res.string.settings_recycle_bin_use_off,
                ),
                checked = enabled,
                onCheckedChange = { action(RecycleBinAction.SetEnabled(it)) },
                enabled = !saving,
            )
            // The key derivation takes seconds
            if (saving) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp))
            }
        }
    }
}

/** How many entries the bin holds; empty - "Empty". The screen of the bin opens from here. */
@Composable
private fun DeletedCard(count: Int, saving: Boolean, onOpen: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        ValueRow(
            title = stringResource(Res.string.settings_recycle_bin_deleted),
            value = if (count == 0) stringResource(Res.string.settings_recycle_bin_empty) else count.toString(),
            onClick = onOpen,
            enabled = count > 0 && !saving,
        )
    }
}
