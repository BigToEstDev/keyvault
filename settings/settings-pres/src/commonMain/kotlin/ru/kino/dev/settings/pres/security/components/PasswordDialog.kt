package ru.kino.dev.settings.pres.security.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import ru.kino.dev.core.CredentialLimits
import ru.kino.dev.core.pres.*
import ru.kino.dev.settings.pres.security.PasswordDialog
import ru.kino.dev.settings.pres.security.PasswordInput
import ru.kino.dev.settings.pres.security.SecurityActions
import ru.kino.dev.ui.components.PasswordField
import ru.kino.dev.ui.res.pluralStringResource
import ru.kino.dev.ui.res.stringResource

/**
 * The master password of the database: the current one first when there is one, then the new one twice - with the
 * limits and the mismatch of `masterpassword` (pass-docs, Step 67). Each field has its eye.
 *
 * While [PasswordDialog.saving] - the password checked, the key derived, the file written - the button shows
 * progress and nothing takes input; the dialog does not close.
 */
@Composable
internal fun SecurityActions.PasswordDialog(dialog: PasswordDialog) {
    val focus = remember { FocusRequester() }
    val enabled = !dialog.saving

    AlertDialog(
        onDismissRequest = { onDismissPassword() },
        title = {
            Text(
                stringResource(
                    if (dialog.needsCurrent) Res.string.settings_security_password_change else Res.string.settings_security_password_add,
                ),
            )
        },
        text = {
            // Here, inside the dialog: it is a window of its own, and the field has to exist before it takes focus
            val keyboard = LocalSoftwareKeyboardController.current
            LaunchedEffect(Unit) {
                focus.requestFocus()
                keyboard?.show()
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (dialog.needsCurrent) {
                    PasswordField(
                        value = dialog.current,
                        onValueChange = { onCurrentPasswordChange(it) },
                        label = stringResource(Res.string.settings_security_password_current),
                        visible = PasswordInput.Current in dialog.visible,
                        onToggleVisible = { onTogglePasswordVisible(PasswordInput.Current) },
                        error = if (dialog.wrongCurrent) stringResource(Res.string.settings_biometric_wrong_password) else null,
                        enabled = enabled,
                        imeAction = ImeAction.Next,
                        modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    )
                }
                PasswordField(
                    value = dialog.password,
                    onValueChange = { onNewPasswordChange(it) },
                    label = stringResource(Res.string.settings_security_password_new),
                    visible = PasswordInput.New in dialog.visible,
                    onToggleVisible = { onTogglePasswordVisible(PasswordInput.New) },
                    error = if (dialog.tooLong) {
                        val max = CredentialLimits.PASSWORD_MAX_CHARS
                        pluralStringResource(Res.plurals.create_password_too_long, max, max)
                    } else {
                        null
                    },
                    hint = if (dialog.tooShort) {
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
                        .then(if (dialog.needsCurrent) Modifier else Modifier.focusRequester(focus)),
                )
                PasswordField(
                    value = dialog.confirm,
                    onValueChange = { onConfirmPasswordChange(it) },
                    label = stringResource(Res.string.create_password_confirm),
                    visible = PasswordInput.Confirm in dialog.visible,
                    onToggleVisible = { onTogglePasswordVisible(PasswordInput.Confirm) },
                    error = if (dialog.mismatch) stringResource(Res.string.create_password_mismatch) else null,
                    enabled = enabled,
                    onImeAction = { if (dialog.canSave) onSavePassword() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged {
                            if (!it.isFocused && dialog.confirm.isNotEmpty()) onConfirmPasswordLeft()
                        },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSavePassword() }, enabled = dialog.canSave) {
                if (dialog.saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = LocalContentColor.current,
                    )
                } else {
                    Text(stringResource(Res.string.action_save))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismissPassword() }, enabled = enabled) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
    )
}
