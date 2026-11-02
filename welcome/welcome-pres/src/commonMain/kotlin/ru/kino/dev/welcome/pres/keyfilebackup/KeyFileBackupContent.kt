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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.kino.dev.core.pres.*
import ru.kino.dev.ui.components.GradientBackground
import ru.kino.dev.ui.components.PrimaryButton
import ru.kino.dev.ui.components.SecondaryButton
import ru.kino.dev.ui.res.stringResource
import ru.kino.dev.welcome.pres.create.CreateProblemText

/** The key file of a new database in one column (pass-docs, `docs/app/mvi-arc.md`). */
@Composable
internal fun KeyFileBackupActions.KeyFileBackupContent(state: KeyFileBackupState) {
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
                text = stringResource(Res.string.key_file_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.key_file_text),
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
                    text = stringResource(Res.string.key_file_create),
                    onClick = ::onContinue,
                    loading = state.stage.busy,
                )
                Spacer(Modifier.height(12.dp))
                SecondaryButton(
                    text = stringResource(Res.string.key_file_pick_existing),
                    onClick = ::onPickExisting,
                    enabled = !state.stage.busy,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.key_file_existing_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                PrimaryButton(
                    text = stringResource(Res.string.action_next),
                    onClick = ::onContinue,
                    loading = state.stage.busy,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.key_file_next_hint),
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
                KeyFileChoice.Created -> stringResource(Res.string.key_file_saved)
                is KeyFileChoice.Picked -> keyFile.name?.let { stringResource(Res.string.key_file_picked, it) }
                    ?: stringResource(Res.string.key_file_picked_unnamed)
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}
