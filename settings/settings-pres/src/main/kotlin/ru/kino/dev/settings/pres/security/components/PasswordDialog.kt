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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import ru.kino.dev.core.CredentialLimits
import ru.kino.dev.core.pres.R
import ru.kino.dev.settings.pres.security.PasswordDialog
import ru.kino.dev.settings.pres.security.SecurityAction
import ru.kino.dev.ui.components.PasswordField

/**
 * The master password of the database: the current one first when there is one, then the new one twice - with the
 * limits and the mismatch of `masterpassword` (pass-docs, Step 67). One eye shows or hides all of them.
 *
 * While [PasswordDialog.saving] - the password checked, the key derived, the file written - the button shows
 * progress and nothing takes input; the dialog does not close.
 */
@Composable
internal fun PasswordDialog(dialog: PasswordDialog, action: (SecurityAction) -> Unit) {
    val focus = remember { FocusRequester() }
    val enabled = !dialog.saving

    AlertDialog(
        onDismissRequest = { action(SecurityAction.DismissPassword) },
        title = {
            Text(
                stringResource(
                    if (dialog.needsCurrent) R.string.settings_security_password_change else R.string.settings_security_password_add,
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
                        onValueChange = { action(SecurityAction.CurrentPasswordChanged(it)) },
                        label = stringResource(R.string.settings_security_password_current),
                        visible = dialog.visible,
                        onToggleVisible = { action(SecurityAction.TogglePasswordVisible) },
                        error = if (dialog.wrongCurrent) stringResource(R.string.settings_biometric_wrong_password) else null,
                        enabled = enabled,
                        imeAction = ImeAction.Next,
                        modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    )
                }
                PasswordField(
                    value = dialog.password,
                    onValueChange = { action(SecurityAction.NewPasswordChanged(it)) },
                    label = stringResource(R.string.settings_security_password_new),
                    visible = dialog.visible,
                    // The eye on the first field only: one for all of them
                    onToggleVisible = if (dialog.needsCurrent) null else ({ action(SecurityAction.TogglePasswordVisible) }),
                    error = if (dialog.tooLong) {
                        val max = CredentialLimits.PASSWORD_MAX_CHARS
                        pluralStringResource(R.plurals.create_password_too_long, max, max)
                    } else {
                        null
                    },
                    hint = if (dialog.tooShort) {
                        val min = CredentialLimits.PASSWORD_MIN_CHARS
                        pluralStringResource(R.plurals.create_password_too_short, min, min)
                    } else {
                        null
                    },
                    enabled = enabled,
                    imeAction = ImeAction.Next,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (dialog.needsCurrent) Modifier else Modifier.focusRequester(focus)),
                )
                PasswordField(
                    value = dialog.confirm,
                    onValueChange = { action(SecurityAction.ConfirmPasswordChanged(it)) },
                    label = stringResource(R.string.create_password_confirm),
                    visible = dialog.visible,
                    onToggleVisible = null,
                    error = if (dialog.mismatch) stringResource(R.string.create_password_mismatch) else null,
                    enabled = enabled,
                    onImeAction = { if (dialog.canSave) action(SecurityAction.SavePassword) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged {
                            if (!it.isFocused && dialog.confirm.isNotEmpty()) action(SecurityAction.ConfirmPasswordLeft)
                        },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { action(SecurityAction.SavePassword) }, enabled = dialog.canSave) {
                if (dialog.saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = LocalContentColor.current,
                    )
                } else {
                    Text(stringResource(R.string.action_save))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { action(SecurityAction.DismissPassword) }, enabled = enabled) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
