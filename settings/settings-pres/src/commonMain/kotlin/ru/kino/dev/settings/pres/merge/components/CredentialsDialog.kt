package ru.kino.dev.settings.pres.merge.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import ru.kino.dev.core.CredentialLimits
import ru.kino.dev.core.pres.Res
import ru.kino.dev.core.pres.action_cancel
import ru.kino.dev.core.pres.action_remove
import ru.kino.dev.core.pres.create_password_too_long
import ru.kino.dev.core.pres.error_invalid_credentials
import ru.kino.dev.core.pres.key_file_title
import ru.kino.dev.core.pres.settings_sync_credentials_hint
import ru.kino.dev.core.pres.settings_sync_import
import ru.kino.dev.core.pres.settings_sync_import_confirm
import ru.kino.dev.core.pres.settings_sync_key_file_none
import ru.kino.dev.core.pres.settings_sync_merge
import ru.kino.dev.core.pres.settings_sync_merge_confirm
import ru.kino.dev.core.pres.settings_sync_password
import ru.kino.dev.settings.pres.merge.CredentialsDialog
import ru.kino.dev.settings.pres.merge.MergeActions
import ru.kino.dev.ui.components.PasswordField
import ru.kino.dev.ui.components.PickerField
import ru.kino.dev.vault.sync.OtherDatabaseKind

/**
 * The password and the key file of the other database - its own, they may differ from those of this one (pass-docs,
 * Step 108). The file picked is named in the title's place under it. Wrong credentials are said under the field.
 *
 * While [CredentialsDialog.merging] - the file read, the key derived, merged, written - the button shows progress and
 * nothing takes input; the dialog does not close.
 *
 * @param pickKeyFile launches the picker of a key file; what is picked comes to [MergeActions.onKeyFilePicked]
 */
@Composable
internal fun MergeActions.CredentialsDialog(dialog: CredentialsDialog, pickKeyFile: () -> Unit) {
    val focus = remember { FocusRequester() }
    val enabled = !dialog.merging
    val copy = dialog.kind == OtherDatabaseKind.Copy

    AlertDialog(
        onDismissRequest = { onDismissCredentials() },
        title = { Text(stringResource(if (copy) Res.string.settings_sync_merge else Res.string.settings_sync_import)) },
        text = {
            LaunchedEffect(Unit) { focus.requestFocus() }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                dialog.fileName?.let { Text(it, style = MaterialTheme.typography.titleSmall) }
                Text(
                    stringResource(Res.string.settings_sync_credentials_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PasswordField(
                    value = dialog.password,
                    onValueChange = { onPasswordChange(it) },
                    label = stringResource(Res.string.settings_sync_password),
                    visible = dialog.passwordVisible,
                    onToggleVisible = { onTogglePasswordVisible() },
                    error = when {
                        dialog.wrongCredentials -> stringResource(Res.string.error_invalid_credentials)
                        dialog.tooLong -> {
                            val max = CredentialLimits.PASSWORD_MAX_CHARS
                            pluralStringResource(Res.plurals.create_password_too_long, max, max)
                        }
                        else -> null
                    },
                    enabled = enabled,
                    onImeAction = { if (dialog.canConfirm) onConfirm() },
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                )
                PickerField(
                    value = dialog.keyFileName
                        ?: dialog.keyFileUri?.substringAfterLast('/')
                        ?: stringResource(Res.string.settings_sync_key_file_none),
                    label = stringResource(Res.string.key_file_title),
                    onClick = pickKeyFile,
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (dialog.keyFileUri != null) {
                    TextButton(onClick = { onRemoveKeyFile() }, enabled = enabled) {
                        Text(stringResource(Res.string.action_remove))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm() }, enabled = dialog.canConfirm) {
                if (dialog.merging) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = LocalContentColor.current,
                    )
                } else {
                    Text(
                        stringResource(
                            if (copy) Res.string.settings_sync_merge_confirm else Res.string.settings_sync_import_confirm,
                        ),
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismissCredentials() }, enabled = enabled) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
    )
}
