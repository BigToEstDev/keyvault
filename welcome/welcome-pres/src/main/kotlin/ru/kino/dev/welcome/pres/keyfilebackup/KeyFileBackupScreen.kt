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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import ru.kino.dev.core.pres.R
import ru.kino.dev.ui.components.GradientBackground
import ru.kino.dev.ui.components.PrimaryButton
import ru.kino.dev.welcome.pres.common.rememberCreateDocument
import ru.kino.dev.welcome.pres.create.CreateDraft
import ru.kino.dev.welcome.pres.create.CreateProblemText
import ru.kino.dev.welcome.pres.create.OverwriteDialog

@Composable
internal fun KeyFileBackupScreen(
    draft: CreateDraft,
    processor: KeyFileBackupProcessor = hiltViewModel<KeyFileBackupProcessor, KeyFileBackupProcessor.Factory>(
        creationCallback = { factory -> factory.create(draft) },
    ),
) {
    val state by processor.collectAsState()
    val pickKeyFile = rememberCreateDocument { processor.action(KeyFileBackupAction.KeyFilePicked(it)) }
    val pickDatabase = rememberCreateDocument { processor.action(KeyFileBackupAction.DatabasePicked(it)) }

    processor.collectSideEffect { effect ->
        when (effect) {
            is KeyFileBackupSideEffect.PickKeyFile -> pickKeyFile(effect.suggestedName)
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

    KeyFileBackupContent(state = state, onContinue = { processor.action(KeyFileBackupAction.Continue) })
}

@Composable
private fun KeyFileBackupContent(state: KeyFileBackupState, onContinue: () -> Unit) {
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
            if (state.keyFileSaved) {
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.key_file_saved),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
            Spacer(Modifier.height(32.dp))

            state.stage.problem?.let {
                CreateProblemText(it)
                Spacer(Modifier.height(8.dp))
            }
            PrimaryButton(
                text = stringResource(
                    if (state.keyFileSaved) R.string.key_file_choose_database else R.string.key_file_save,
                ),
                onClick = onContinue,
                loading = state.stage.busy,
            )
        }
    }
}
