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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import ru.kino.dev.core.CredentialLimits
import ru.kino.dev.core.pres.Res
import ru.kino.dev.core.pres.action_cancel
import ru.kino.dev.core.pres.action_synchronize
import ru.kino.dev.core.pres.create_password_too_long
import ru.kino.dev.core.pres.entry_database_fallback
import ru.kino.dev.core.pres.entry_password
import ru.kino.dev.core.pres.error_invalid_credentials
import ru.kino.dev.core.pres.settings_sync_both_ways
import ru.kino.dev.core.pres.settings_sync_credentials_hint
import ru.kino.dev.core.pres.settings_sync_import
import ru.kino.dev.core.pres.settings_sync_import_confirm
import ru.kino.dev.core.pres.settings_sync_merge
import ru.kino.dev.core.pres.settings_sync_merge_confirm
import ru.kino.dev.settings.pres.merge.CredentialsDialog
import ru.kino.dev.settings.pres.merge.MergeActions
import ru.kino.dev.ui.components.DatabaseNameLink
import ru.kino.dev.ui.components.KeyFileChoice
import ru.kino.dev.ui.components.PasswordField
import ru.kino.dev.ui.res.pluralStringResource
import ru.kino.dev.ui.res.stringResource
import ru.kino.dev.ui.theme.LayoutType
import ru.kino.dev.ui.theme.LocalLayoutType
import ru.kino.dev.vault.sync.OtherDatabaseKind

/**
 * The master password and the key file of the other database - its own, they may differ from those of this one
 * (pass-docs, Step 108). As on the sign-in screen: the name of the file picked is a link - a tap picks another, what
 * was typed stays; "Has a key file", then its name with "Unlink". Wrong credentials are said under the field.
 *
 * While [CredentialsDialog.merging] - the file read, the key derived, merged, written - the button shows progress and
 * nothing takes input; the dialog does not close.
 *
 * @param pickDatabase launches the picker of a database for the same card; what is picked comes to
 * [MergeActions.onDatabasePicked]
 * @param pickKeyFile launches the picker of a key file; what is picked comes to [MergeActions.onKeyFilePicked]
 */
@Composable
internal fun MergeActions.CredentialsDialog(
    dialog: CredentialsDialog,
    pickDatabase: () -> Unit,
    pickKeyFile: () -> Unit,
) {
    val focus = remember { FocusRequester() }
    val enabled = !dialog.merging

    AlertDialog(
        onDismissRequest = { onDismissCredentials() },
        title = { Text(stringResource(dialog.kind.title)) },
        text = {
            LaunchedEffect(Unit) { focus.requestFocus() }
            // At the start, as a dialog reads; the sign-in screen is centred
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DatabaseNameLink(
                    name = dialog.fileName ?: stringResource(Res.string.entry_database_fallback),
                    onChange = pickDatabase,
                    enabled = enabled,
                    style = MaterialTheme.typography.titleMedium,
                    alignment = Alignment.Start,
                    // A phone has no room to spare in a dialog, and the link says it is one
                    hint = LocalLayoutType.current == LayoutType.Tablet,
                )
                Text(
                    stringResource(Res.string.settings_sync_credentials_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
                PasswordField(
                    value = dialog.password,
                    onValueChange = { onPasswordChange(it) },
                    label = stringResource(Res.string.entry_password),
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
                KeyFileChoice(
                    attached = dialog.keyFileUri != null,
                    keyFileName = dialog.keyFileName,
                    onAttach = pickKeyFile,
                    onRemove = { onRemoveKeyFile() },
                    enabled = enabled,
                    alignment = Alignment.Start,
                )
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
                    Text(stringResource(dialog.kind.confirm))
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

/** The title of the dialog - the card it came from. */
private val OtherDatabaseKind.title: StringResource
    get() = when (this) {
        OtherDatabaseKind.Copy -> Res.string.settings_sync_merge
        OtherDatabaseKind.CopyBothWays -> Res.string.settings_sync_both_ways
        OtherDatabaseKind.Import -> Res.string.settings_sync_import
    }

/** What its button does. */
private val OtherDatabaseKind.confirm: StringResource
    get() = when (this) {
        OtherDatabaseKind.Copy -> Res.string.settings_sync_merge_confirm
        OtherDatabaseKind.CopyBothWays -> Res.string.action_synchronize
        OtherDatabaseKind.Import -> Res.string.settings_sync_import_confirm
    }
