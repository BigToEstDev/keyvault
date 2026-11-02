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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import ru.kino.dev.core.CredentialLimits
import ru.kino.dev.core.pres.*
import ru.kino.dev.ui.components.GradientBackground
import ru.kino.dev.ui.components.PasswordField
import ru.kino.dev.ui.components.PrimaryButton
import ru.kino.dev.ui.res.pluralStringResource
import ru.kino.dev.ui.res.stringResource
import ru.kino.dev.welcome.pres.create.CreateProblemText

/** Setting the master password in one column (pass-docs, `docs/app/mvi-arc.md`). */
@Composable
internal fun MasterPasswordActions.MasterPasswordContent(state: MasterPasswordState) {
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
                text = stringResource(Res.string.create_password_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.create_password_text),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))

            PasswordField(
                value = state.password,
                onValueChange = ::onPasswordChange,
                label = stringResource(Res.string.entry_password),
                visible = state.passwordVisible,
                onToggleVisible = ::onTogglePasswordVisible,
                error = if (state.tooLong) {
                    val max = CredentialLimits.PASSWORD_MAX_CHARS
                    pluralStringResource(Res.plurals.create_password_too_long, max, max)
                } else {
                    null
                },
                hint = if (state.tooShort) {
                    val min = CredentialLimits.PASSWORD_MIN_CHARS
                    pluralStringResource(Res.plurals.create_password_too_short, min, min)
                } else {
                    null
                },
                enabled = enabled,
                imeAction = ImeAction.Next,
                newPassword = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus),
            )
            Spacer(Modifier.height(8.dp))
            // Always there, shown or hidden together with the password: one toggle for both fields
            PasswordField(
                value = state.confirm,
                onValueChange = ::onConfirmChange,
                label = stringResource(Res.string.create_password_confirm),
                visible = state.passwordVisible,
                onToggleVisible = null,
                error = if (state.mismatch) stringResource(Res.string.create_password_mismatch) else null,
                enabled = enabled,
                onImeAction = ::onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged {
                        if (!it.isFocused && state.confirm.isNotEmpty()) onConfirmLeft()
                    },
            )

            Spacer(Modifier.height(16.dp))
            KeyFileSwitch(
                checked = state.withKeyFile,
                enabled = enabled,
                onChange = ::onKeyFileToggle,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(Res.string.create_sync_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))

            state.stage.problem?.let {
                CreateProblemText(it)
                Spacer(Modifier.height(8.dp))
            }
            PrimaryButton(
                text = stringResource(Res.string.action_next),
                onClick = ::onNext,
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
                text = stringResource(Res.string.create_key_file_switch),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(Res.string.create_key_file_switch_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(16.dp))
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}
