package ru.kino.dev.welcome.pres.keyfilebackup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import ru.kino.dev.core.pres.R
import ru.kino.dev.ui.components.GradientBackground
import ru.kino.dev.ui.components.PrimaryButton
import ru.kino.dev.ui.components.SecondaryButton
import ru.kino.dev.ui.files.rememberCreateDocument
import ru.kino.dev.ui.files.rememberOpenDocument
import ru.kino.dev.welcome.pres.create.CreateDraft
import ru.kino.dev.welcome.pres.create.CreateProblemText
import ru.kino.dev.welcome.pres.create.OverwriteDialog

@Composable
internal fun KeyFileBackupScreen(
    draft: CreateDraft,
    processor: KeyFileBackupProcessor = koinViewModel { parametersOf(draft) },
) {
    val state by processor.collectAsState()
    val pickKeyFile = rememberCreateDocument { processor.action(KeyFileBackupAction.KeyFilePicked(it)) }
    val pickExisting = rememberOpenDocument { processor.action(KeyFileBackupAction.ExistingPicked(it)) }
    val pickDatabase = rememberCreateDocument { processor.action(KeyFileBackupAction.DatabasePicked(it)) }

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
            onConfirm = { processor.action(KeyFileBackupAction.OverwriteConfirmed) },
            onDismiss = { processor.action(KeyFileBackupAction.OverwriteDismissed) },
        )
    }

    KeyFileBackupContent(
        state = state,
        onContinue = { processor.action(KeyFileBackupAction.Continue) },
        onPickExisting = { processor.action(KeyFileBackupAction.PickExisting) },
    )
}

@Composable
private fun KeyFileBackupContent(state: KeyFileBackupState, onContinue: () -> Unit, onPickExisting: () -> Unit) {
    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Key,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.key_file_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.key_file_text),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            state.keyFile?.let { KeyFileReady(it) }
            Spacer(Modifier.height(32.dp))

            state.stage.problem?.let {
                CreateProblemText(it)
                Spacer(Modifier.height(8.dp))
            }
            if (state.keyFile == null) {
                PrimaryButton(
                    text = stringResource(R.string.key_file_create),
                    onClick = onContinue,
                    loading = state.stage.busy,
                )
                Spacer(Modifier.height(12.dp))
                SecondaryButton(
                    text = stringResource(R.string.key_file_pick_existing),
                    onClick = onPickExisting,
                    enabled = !state.stage.busy,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.key_file_existing_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                PrimaryButton(
                    text = stringResource(R.string.action_next),
                    onClick = onContinue,
                    loading = state.stage.busy,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.key_file_next_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun KeyFileReady(keyFile: KeyFileChoice) {
    Spacer(Modifier.height(16.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = when (keyFile) {
                KeyFileChoice.Created -> stringResource(R.string.key_file_saved)
                is KeyFileChoice.Picked -> keyFile.name?.let { stringResource(R.string.key_file_picked, it) }
                    ?: stringResource(R.string.key_file_picked_unnamed)
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}
