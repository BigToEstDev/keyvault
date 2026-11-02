package ru.kino.dev.welcome.pres.common.passwordentry

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import ru.kino.dev.core.pres.*
import ru.kino.dev.ui.components.PasswordField
import ru.kino.dev.ui.components.PrimaryButton
import ru.kino.dev.ui.components.Reveal
import ru.kino.dev.ui.components.SecondaryButton
import ru.kino.dev.ui.res.stringResource
import ru.kino.dev.ui.theme.DISABLED_ALPHA
import ru.kino.dev.welcome.pres.common.BiometricNotice
import ru.kino.dev.welcome.pres.common.EntryProblem
import ru.kino.dev.welcome.pres.common.PasswordEntry
import ru.kino.dev.welcome.pres.common.hidesPassword
import ru.kino.dev.welcome.pres.common.isAboutKeyFile

// The pieces of the layouts of entering a password (pass-docs, Step 94): the layouts place them, these draw them

/** The shield alone while the state is read - as large as on the welcome screen. */
internal val SHIELD_SIZE_LARGE = 140.dp

/** What was read coming into view - one length for all of it, so it goes as one. */
internal const val READ_MILLIS = 400

/** What the screen is about: the subtitle of the locked screen, if any, and the name of the database. */
@Composable
internal fun PasswordEntryActions.DatabaseHeader(
    databaseName: String,
    subtitle: String?,
    abilities: PasswordEntryAbilities,
    enabled: Boolean,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        subtitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
        }
        DatabaseName(name = databaseName, changeable = abilities.changeDatabase, enabled = enabled)
    }
}

/**
 * What is asked to get in: the password and the ways next to it; a problem with the database file takes the place
 * of all of it - the one fades into the other.
 */
@Composable
internal fun PasswordEntryActions.EntryForm(entry: PasswordEntry, submitText: String, abilities: PasswordEntryAbilities) {
    AnimatedContent(
        targetState = entry,
        contentKey = { it.problem.hidesPassword },
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "file problem",
    ) { shown ->
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (shown.problem.hidesPassword) {
                FileProblem(entry = shown, abilities = abilities)
            } else {
                PasswordInput(entry = shown, submitText = submitText)
                BiometricSignInRow(entry = shown)
                Spacer(Modifier.height(16.dp))
                KeyFileRow(entry = shown, changeable = abilities.changeKeyFile)
                if (abilities.createDatabase) {
                    Spacer(Modifier.height(24.dp))
                    CreateDatabaseButton(enabled = !shown.busy)
                }
            }
        }
    }
}

/**
 * The name: a link in the accent when it picks another database ([changeable]); otherwise in the accent, not a
 * link. While the database opens the link and its hint grey out rather than go - what is under them does not jump
 * (pass-docs, Step 72, fix 4).
 */
@Composable
internal fun PasswordEntryActions.DatabaseName(name: String, changeable: Boolean, enabled: Boolean) {
    val colors = MaterialTheme.colorScheme
    val disabled = colors.onSurface.copy(alpha = DISABLED_ALPHA)
    Text(
        text = name,
        style = MaterialTheme.typography.headlineSmall,
        color = if (changeable && !enabled) disabled else colors.primary,
        textDecoration = if (changeable) TextDecoration.Underline else null,
        textAlign = TextAlign.Center,
        modifier = if (changeable) Modifier.clickable(enabled = enabled, onClick = ::onChangeDatabase) else Modifier,
    )
    if (changeable) {
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(Res.string.entry_change_database_hint),
            style = MaterialTheme.typography.bodySmall,
            color = if (enabled) colors.onSurfaceVariant else disabled,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
internal fun PasswordEntryActions.PasswordInput(entry: PasswordEntry, submitText: String) {
    val focus = remember { FocusRequester() }
    // The keyboard right away: typing the password is the one thing to do here
    LaunchedEffect(Unit) { focus.requestFocus() }

    PasswordField(
        value = entry.password,
        onValueChange = ::onPasswordChange,
        label = stringResource(Res.string.entry_password),
        visible = entry.passwordVisible,
        onToggleVisible = ::onTogglePasswordVisible,
        error = when (entry.problem) {
            EntryProblem.InvalidCredentials -> stringResource(Res.string.error_invalid_credentials)
            else -> null
        },
        enabled = !entry.busy,
        onImeAction = ::onSubmit,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focus),
    )
    Spacer(Modifier.height(16.dp))
    PrimaryButton(text = submitText, onClick = ::onSubmit, loading = entry.busy)
    Reveal(visible = entry.problem == EntryProblem.Unexpected) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(8.dp))
            ErrorText(stringResource(Res.string.error_unexpected))
        }
    }
}

/**
 * A second way in, next to the password, never instead of it - and only on a tap. The button and what is said
 * under it open out and fold away, the gaps with them (pass-docs, Step 72, fix 4).
 */
@Composable
internal fun PasswordEntryActions.BiometricSignInRow(entry: PasswordEntry) {
    Reveal(visible = entry.biometricOn) {
        Column {
            Spacer(Modifier.height(12.dp))
            SecondaryButton(
                text = stringResource(Res.string.entry_biometric),
                onClick = ::onBiometric,
                enabled = !entry.busy,
            )
        }
    }
    Reveal(entry.biometricNotice) { notice ->
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(8.dp))
            ErrorText(
                stringResource(
                    when (notice) {
                        BiometricNotice.DeviceChanged -> Res.string.biometric_notice_device_changed
                        BiometricNotice.PasswordChanged -> Res.string.biometric_notice_password_changed
                        BiometricNotice.Failed -> Res.string.biometric_notice_failed
                    },
                ),
            )
        }
    }
}

/**
 * The key file: its name, and the way to pick it again when it cannot be read. "Attach" and the attached file
 * fade into each other, the problem under it opens out (pass-docs, Step 72, fix 4).
 *
 * @param changeable a key file is attached and removed here; otherwise - the locked screen - there is no removing
 * it, and no attaching one to a database opened without
 */
@Composable
internal fun PasswordEntryActions.KeyFileRow(entry: PasswordEntry, changeable: Boolean) {
    AnimatedContent(
        targetState = entry,
        contentKey = { it.keyFileUri != null },
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "key file",
    ) { shown ->
        if (shown.keyFileUri == null) {
            if (changeable) {
                TextButton(onClick = ::onAttachKeyFile, enabled = !shown.busy) {
                    Icon(imageVector = Icons.Outlined.Key, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(Res.string.entry_key_file_add))
                }
            }
        } else {
            AttachedKeyFile(entry = shown, changeable = changeable)
        }
    }
}

@Composable
private fun PasswordEntryActions.AttachedKeyFile(entry: PasswordEntry, changeable: Boolean) {
    // Greys out with the buttons under it while the database opens (pass-docs, Step 72, fix 3)
    val color = if (entry.busy) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_ALPHA)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.Key,
                contentDescription = null,
                tint = color,
            )
            Spacer(Modifier.size(8.dp))
            Text(
                text = stringResource(
                    Res.string.entry_key_file_name,
                    entry.keyFileName ?: stringResource(Res.string.entry_key_file_unnamed),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = color,
            )
        }
        Reveal(entry.problem.takeIf { it.isAboutKeyFile }) { problem ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(4.dp))
                ErrorText(
                    when (problem) {
                        is EntryProblem.KeyFileTooLarge ->
                            stringResource(Res.string.error_key_file_too_large, problem.maxMebibytes.toInt())
                        else -> stringResource(Res.string.error_key_file_unreachable)
                    },
                )
                TextButton(onClick = ::onAttachKeyFile, enabled = !entry.busy) {
                    Text(stringResource(Res.string.action_pick_another_key_file))
                }
            }
        }
        if (changeable) {
            TextButton(onClick = ::onRemoveKeyFile, enabled = !entry.busy) {
                Text(stringResource(Res.string.entry_key_file_remove))
            }
        }
    }
}

/** A button, not a link: it is the way out when the remembered database is gone. */
@Composable
internal fun PasswordEntryActions.CreateDatabaseButton(enabled: Boolean) {
    SecondaryButton(
        text = stringResource(Res.string.entry_create_database),
        onClick = ::onCreateDatabase,
        enabled = enabled,
    )
}

/** A problem with the database file, in place of the password: what is wrong and the ways on. */
@Composable
internal fun PasswordEntryActions.FileProblem(entry: PasswordEntry, abilities: PasswordEntryAbilities) {
    val problem = entry.problem
    ErrorText(
        when (problem) {
            EntryProblem.UnsupportedFile -> stringResource(Res.string.error_unsupported_file)
            EntryProblem.CorruptedFile -> stringResource(Res.string.error_corrupted_file)
            else -> stringResource(Res.string.error_database_unreachable)
        },
    )
    Spacer(Modifier.height(24.dp))
    // Another try changes nothing for a file of a kind not read
    if (problem != EntryProblem.UnsupportedFile) {
        PrimaryButton(text = stringResource(Res.string.action_retry), onClick = ::onRetry, loading = entry.busy)
        Spacer(Modifier.height(12.dp))
    }
    if (abilities.changeDatabase) {
        SecondaryButton(
            text = stringResource(Res.string.action_pick_another_file),
            onClick = ::onChangeDatabase,
            enabled = !entry.busy,
        )
    }
    if (abilities.createDatabase) {
        Spacer(Modifier.height(12.dp))
        CreateDatabaseButton(enabled = !entry.busy)
    }
}

@Composable
internal fun ErrorText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
    )
}
