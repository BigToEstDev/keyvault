package ru.kino.dev.welcome.pres.keyfilebackup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import ru.kino.dev.pane.koinProcessor
import ru.kino.dev.ui.files.DocumentKind
import ru.kino.dev.ui.files.rememberCreateDocument
import ru.kino.dev.ui.files.rememberOpenDocument
import ru.kino.dev.welcome.pres.create.CreateDraft
import ru.kino.dev.welcome.pres.create.OverwriteDialog

@Composable
internal fun KeyFileBackupScreen(
    draft: CreateDraft,
    processor: KeyFileBackupProcessor = koinProcessor(draft),
) {
    val state by processor.collectAsState()
    val pickKeyFile = rememberCreateDocument(DocumentKind.KeyFile) { processor.onKeyFilePicked(it) }
    val pickExisting = rememberOpenDocument(DocumentKind.KeyFile) { processor.onExistingPicked(it) }
    val pickDatabase = rememberCreateDocument(DocumentKind.Database) { processor.onDatabasePicked(it) }

    processor.collectSideEffect { effect ->
        when (effect) {
            is KeyFileBackupSideEffect.PickKeyFile -> pickKeyFile(effect.suggestedName)
            KeyFileBackupSideEffect.PickExistingKeyFile -> pickExisting()
            is KeyFileBackupSideEffect.PickDatabase -> pickDatabase(effect.suggestedName)
        }
    }

    state.stage.overwrite?.let {
        OverwriteDialog(
            overwrite = it,
            onConfirm = processor::onOverwriteConfirmed,
            onDismiss = processor::onOverwriteDismissed,
        )
    }

    processor.KeyFileBackupContent(state)
}
