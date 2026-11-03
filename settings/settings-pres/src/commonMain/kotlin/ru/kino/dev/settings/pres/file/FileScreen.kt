package ru.kino.dev.settings.pres.file

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.orbitmvi.orbit.compose.collectAsState
import ru.kino.dev.core.Compression
import ru.kino.dev.core.KdbxVersion
import ru.kino.dev.core.pres.*
import ru.kino.dev.pane.koinProcessor
import ru.kino.dev.settings.pres.file.components.FileSkeleton
import ru.kino.dev.ui.components.BackTopBar
import ru.kino.dev.ui.components.GradientBackground
import ru.kino.dev.ui.components.RadioGroup
import ru.kino.dev.ui.components.SnackbarMessage
import ru.kino.dev.ui.components.TextInputDialog
import ru.kino.dev.ui.components.ValueRow
import ru.kino.dev.ui.components.skeletonBrush
import ru.kino.dev.ui.res.stringResource
import ru.kino.dev.vault.file.DatabaseFileInfo

/**
 * The file of the database: its name inside the file - tapped, a dialog; the name of the file and the format -
 * to read; the compression (pass-docs, Step 63).
 */
@Composable
internal fun FileScreen(
    dbKey: String,
    processor: FileProcessor = koinProcessor(dbKey),
) {
    val state by processor.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { processor.onLoad() }

    val message = state.message?.let {
        stringResource(
            when (it) {
                FileMessage.NotSaved -> Res.string.message_file_not_saved
                FileMessage.Failed -> Res.string.error_unexpected
            },
        )
    }
    SnackbarMessage(message, snackbar) { processor.onMessageShown() }

    GradientBackground {
        Scaffold(
            // The backdrop is the gradient behind
            containerColor = Color.Transparent,
            topBar = {
                BackTopBar(
                    title = stringResource(Res.string.settings_file),
                    onBack = processor::onBack,
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
                val file = state.file
                if (file == null) {
                    // Until read there is nothing to show for real: the shapes of the screen
                    item(key = "skeleton") { FileSkeleton(skeletonBrush()) }
                } else {
                    fileRows(file, state.savingCompression, processor)
                }
            }
        }
    }

    state.renaming?.let { dialog ->
        TextInputDialog(
            title = stringResource(Res.string.settings_file_name),
            value = dialog.name,
            onValueChange = processor::onRenameChange,
            label = stringResource(Res.string.settings_file_name),
            confirmText = stringResource(Res.string.action_save),
            dismissText = stringResource(Res.string.action_cancel),
            onConfirm = processor::onRenameConfirm,
            onDismiss = processor::onRenameDismiss,
            error = if (dialog.empty) stringResource(Res.string.error_name_empty) else null,
            loading = dialog.saving,
        )
    }
}

private fun LazyListScope.fileRows(
    file: DatabaseFileInfo,
    savingCompression: Boolean,
    actions: FileActions,
) {
    item(key = "name") {
        Card(modifier = Modifier.fillMaxWidth()) {
            // The name under the title, not at the end: a name is often longer than a value
            ValueRow(
                title = stringResource(Res.string.settings_file_name),
                text = file.name,
                value = "",
                onClick = { actions.onRename() },
                enabled = !savingCompression,
                // Without it the row reads as a value to look at, not one to change
                icon = Icons.Filled.Edit,
            )
        }
    }
    item(key = "file") {
        Card(modifier = Modifier.fillMaxWidth()) {
            ValueRow(
                title = stringResource(Res.string.settings_file_file),
                text = file.fileName ?: stringResource(Res.string.settings_file_file_unknown),
                value = "",
                onClick = null,
            )
        }
    }
    item(key = "format") {
        Card(modifier = Modifier.fillMaxWidth()) {
            ValueRow(
                title = stringResource(Res.string.settings_file_format),
                value = when (file.version) {
                    KdbxVersion.V40 -> FORMAT_40
                    KdbxVersion.V41 -> FORMAT_41
                },
                // The core writes 4.1 alone: a file of 4.0 becomes 4.1 with its next save
                text = if (file.version == KdbxVersion.V40) stringResource(Res.string.settings_file_format_upgrade) else null,
                onClick = null,
            )
        }
    }
    item(key = "compression") {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                RadioGroup(
                    title = stringResource(Res.string.settings_file_compression),
                    options = listOf(Compression.GZip, Compression.None),
                    selected = file.compression,
                    label = { compressionLabel(it) },
                    onSelect = { actions.onSelectCompression(it) },
                    // While the file is written with the one chosen, the choice waits
                    enabled = !savingCompression,
                    text = stringResource(Res.string.settings_file_compression_hint),
                )
                if (savingCompression) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
                }
            }
        }
    }
}

@Composable
private fun compressionLabel(compression: Compression): String = when (compression) {
    Compression.GZip -> stringResource(Res.string.settings_file_compression_gzip)
    Compression.None -> stringResource(Res.string.settings_file_compression_none)
}

/** The names of the formats - the same in every language. */
private const val FORMAT_40 = "KDBX 4.0"
private const val FORMAT_41 = "KDBX 4.1"
