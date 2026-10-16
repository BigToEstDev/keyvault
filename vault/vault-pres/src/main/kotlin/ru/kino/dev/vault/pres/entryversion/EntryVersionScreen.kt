package ru.kino.dev.vault.pres.entryversion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import ru.kino.dev.vault.pres.common.EntryReadFields
import ru.kino.dev.vault.pres.common.EntrySkeleton
import ru.kino.dev.vault.pres.common.EntryTopBar
import ru.kino.dev.vault.pres.common.RestoreVersionDialog
import ru.kino.dev.vault.pres.common.entryTimeText

@Composable
internal fun EntryVersionScreen(
    dbKey: String,
    entryId: String,
    index: Int,
    processor: EntryVersionProcessor = hiltViewModel<EntryVersionProcessor, EntryVersionProcessor.Factory>(
        creationCallback = { factory -> factory.create(dbKey, entryId, index) },
    ),
) {
    val state by processor.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { processor.action(EntryVersionAction.Load) }

    val message = state.message?.let {
        stringResource(
            when (it) {
                EntryVersionMessage.LoadFailed -> R.string.entry_message_load_failed
                EntryVersionMessage.NotSaved -> R.string.vault_message_not_saved
                EntryVersionMessage.Unexpected -> R.string.error_unexpected
            },
        )
    }
    SnackbarMessage(message, snackbar) { processor.action(EntryVersionAction.MessageShown) }

    val version = state.version
    val modified = version?.times?.modified
    // While the version is read, one brush for the title and the fields - their skeletons run together
    val skeleton = if (version == null && state.loading) skeletonBrush() else null

    GradientBackground {
        Scaffold(
            // The backdrop is the gradient behind
            containerColor = Color.Transparent,
            topBar = {
                // When the version was saved - the name of this screen
                EntryTopBar(
                    title = modified?.let(::entryTimeText).orEmpty(),
                    onBack = { processor.action(EntryVersionAction.Back) },
                    skeleton = skeleton,
                )
            },
            floatingActionButton = {
                if (modified != null) {
                    FloatingActionButton(onClick = { processor.action(EntryVersionAction.Restore) }) {
                        Icon(Icons.Filled.History, contentDescription = stringResource(R.string.history_restore))
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
                    // No group - a version does not keep one; no "copy" - restore it to copy (owner, 17.10.26)
                    version != null -> EntryReadFields(
                        content = version.content,
                        passwordVisible = state.passwordVisible,
                        shownOwn = state.shownOwn,
                        onTogglePassword = { processor.action(EntryVersionAction.TogglePassword) },
                        onToggleOwn = { processor.action(EntryVersionAction.ToggleOwn(it)) },
                        copyable = false,
                    )
                    skeleton != null -> EntrySkeleton(brush = skeleton)
                }
            }
        }

        val restoring = state.restoring
        if (restoring != null && modified != null) {
            RestoreVersionDialog(
                modified = modified,
                restoring = restoring,
                onConfirm = { processor.action(EntryVersionAction.DialogConfirm) },
                onDismiss = { processor.action(EntryVersionAction.DialogDismiss) },
            )
        }
    }
}

/** Room around the cards; at the bottom - for the fab, so it never covers the last field. */
private val CONTENT_PADDING = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp)
