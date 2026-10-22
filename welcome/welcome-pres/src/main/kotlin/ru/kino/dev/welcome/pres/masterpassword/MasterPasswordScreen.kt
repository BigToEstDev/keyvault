package ru.kino.dev.welcome.pres.masterpassword

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import ru.kino.dev.core.CredentialLimits
import ru.kino.dev.core.pres.R
import ru.kino.dev.ui.components.GradientBackground
import ru.kino.dev.ui.components.PasswordField
import ru.kino.dev.ui.components.PrimaryButton
import ru.kino.dev.ui.files.rememberCreateDocument
import ru.kino.dev.welcome.pres.create.CreateDraft
import ru.kino.dev.welcome.pres.create.CreateProblemText
import ru.kino.dev.welcome.pres.create.OverwriteDialog

@Composable
internal fun MasterPasswordScreen(
    draft: CreateDraft,
    processor: MasterPasswordProcessor = hiltViewModel<MasterPasswordProcessor, MasterPasswordProcessor.Factory>(
        creationCallback = { factory -> factory.create(draft) },
    ),
) {
    val state by processor.collectAsState()
    val pickDatabase = rememberCreateDocument { processor.action(MasterPasswordAction.DatabasePicked(it)) }

    processor.collectSideEffect { effect ->
        when (effect) {
            is MasterPasswordSideEffect.PickDatabase -> pickDatabase(effect.suggestedName)
        }
    }

    state.stage.overwrite?.let {
        OverwriteDialog(
            overwrite = it,
            onConfirm = { processor.action(MasterPasswordAction.OverwriteConfirmed) },
            onDismiss = { processor.action(MasterPasswordAction.OverwriteDismissed) },
        )
    }

    MasterPasswordContent(state = state, onAction = processor::action)
}

@Composable
private fun MasterPasswordContent(state: MasterPasswordState, onAction: (MasterPasswordAction) -> Unit) {
    val focus = remember { FocusRequester() }
    // The keyboard right away
    LaunchedEffect(Unit) { focus.requestFocus() }
    val enabled = !state.stage.busy

    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.create_password_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.create_password_text),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))

            PasswordField(
                value = state.password,
                onValueChange = { onAction(MasterPasswordAction.PasswordChanged(it)) },
                label = stringResource(R.string.entry_password),
                visible = state.passwordVisible,
                onToggleVisible = { onAction(MasterPasswordAction.TogglePasswordVisible) },
                error = if (state.tooLong) {
                    val max = CredentialLimits.PASSWORD_MAX_CHARS
                    pluralStringResource(R.plurals.create_password_too_long, max, max)
                } else {
                    null
                },
                hint = if (state.tooShort) {
                    val min = CredentialLimits.PASSWORD_MIN_CHARS
                    pluralStringResource(R.plurals.create_password_too_short, min, min)
                } else {
                    null
                },
                enabled = enabled,
                imeAction = ImeAction.Next,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus),
            )
            Spacer(Modifier.height(8.dp))
            // Always there, shown or hidden together with the password: one toggle for both fields
            PasswordField(
                value = state.confirm,
                onValueChange = { onAction(MasterPasswordAction.ConfirmChanged(it)) },
                label = stringResource(R.string.create_password_confirm),
                visible = state.passwordVisible,
                onToggleVisible = null,
                error = if (state.mismatch) stringResource(R.string.create_password_mismatch) else null,
                enabled = enabled,
                onImeAction = { onAction(MasterPasswordAction.Next) },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged {
                        if (!it.isFocused && state.confirm.isNotEmpty()) onAction(MasterPasswordAction.ConfirmLeft)
                    },
            )

            Spacer(Modifier.height(16.dp))
            KeyFileSwitch(
                checked = state.withKeyFile,
                enabled = enabled,
                onChange = { onAction(MasterPasswordAction.KeyFileToggled(it)) },
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.create_sync_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))

            state.stage.problem?.let {
                CreateProblemText(it)
                Spacer(Modifier.height(8.dp))
            }
            PrimaryButton(
                text = stringResource(R.string.action_next),
                onClick = { onAction(MasterPasswordAction.Next) },
                enabled = state.canGoOn,
                loading = state.stage.busy,
            )
        }
    }
}

@Composable
private fun KeyFileSwitch(checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.create_key_file_switch),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.create_key_file_switch_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(16.dp))
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}
