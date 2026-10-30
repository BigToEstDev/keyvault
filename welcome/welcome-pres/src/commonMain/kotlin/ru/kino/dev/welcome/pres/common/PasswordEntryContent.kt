package ru.kino.dev.welcome.pres.common

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import ru.kino.dev.core.pres.*
import ru.kino.dev.ui.components.GradientBackground
import ru.kino.dev.ui.components.PasswordField
import ru.kino.dev.ui.components.PrimaryButton
import ru.kino.dev.ui.components.Reveal
import ru.kino.dev.ui.components.SecondaryButton
import ru.kino.dev.ui.components.ShieldImage
import ru.kino.dev.ui.res.stringResource
import ru.kino.dev.ui.theme.DISABLED_ALPHA

/**
 * The layout of entering a password, shared by the unlock and the locked screens - one layout, two
 * processors (pass-docs, `decisions.md`, "`unlock` и `locked` — два экрана"). No state of its own.
 *
 * @param databaseName the name of the database, shown as the title
 * @param onDatabaseClick null when the database cannot be changed here; otherwise the name is a link that
 * picks another file
 * @param onPickAnotherDatabase null when there is no other database to pick - the locked screen
 * @param onRetry "retry" on a problem with the database file
 * @param onCreateDatabase null where a new database makes no sense - the locked screen, the database is open;
 * otherwise always there: without it a remembered database that is gone leaves no way on
 * @param onRemoveKeyFile null where the key file is the one the database was opened with - the locked
 * screen: not removed or attached there, only picked again when it cannot be read (pass-docs, Step 46)
 * @param locked the locked screen: the name in the accent, though not a link
 * @param read the state is read: until then the shield alone, large, in the middle - not the defaults drawn
 * as the database; once read the shield shrinks and the rest opens out under it, so the shield slides to its
 * place (pass-docs, Step 72, fix 4)
 */
@Composable
internal fun PasswordEntryContent(
    databaseName: String,
    entry: PasswordEntry,
    submitText: String,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisible: () -> Unit,
    onSubmit: () -> Unit,
    onAttachKeyFile: () -> Unit,
    onRemoveKeyFile: (() -> Unit)?,
    onBiometric: () -> Unit,
    onDatabaseClick: (() -> Unit)?,
    onPickAnotherDatabase: (() -> Unit)?,
    onRetry: () -> Unit,
    onCreateDatabase: (() -> Unit)?,
    read: Boolean,
    subtitle: String? = null,
    locked: Boolean = false,
) {
    val shieldSize by animateDpAsState(
        targetValue = if (read) SHIELD_SIZE else SHIELD_SIZE_READING,
        animationSpec = tween(READ_MILLIS),
        label = "shield",
    )

    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            ShieldImage(modifier = Modifier.size(shieldSize))
            // The column is centred: as this opens out, the shield moves up to its place
            AnimatedVisibility(
                visible = read,
                enter = expandVertically(tween(READ_MILLIS)) + fadeIn(tween(READ_MILLIS)),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(24.dp))
                    subtitle?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    DatabaseName(name = databaseName, accent = locked, onClick = onDatabaseClick, enabled = !entry.busy)
                    Spacer(Modifier.height(32.dp))

                    // A problem with the file takes the place of the password - the one fades into the other
                    AnimatedContent(
                        targetState = entry,
                        contentKey = { it.problem.hidesPassword },
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "file problem",
                    ) { shown ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (shown.problem.hidesPassword) {
                                FileProblem(
                                    problem = shown.problem,
                                    onRetry = onRetry.takeUnless { shown.problem == EntryProblem.UnsupportedFile },
                                    onPickAnother = onPickAnotherDatabase,
                                    onCreateDatabase = onCreateDatabase,
                                    busy = shown.busy,
                                )
                            } else {
                                PasswordInput(
                                    entry = shown,
                                    submitText = submitText,
                                    onPasswordChange = onPasswordChange,
                                    onTogglePasswordVisible = onTogglePasswordVisible,
                                    onSubmit = onSubmit,
                                )
                                BiometricSignInRow(entry = shown, onBiometric = onBiometric)
                                Spacer(Modifier.height(16.dp))
                                KeyFileRow(
                                    entry = shown,
                                    onAttach = onAttachKeyFile,
                                    onRemove = onRemoveKeyFile,
                                )
                                onCreateDatabase?.let {
                                    Spacer(Modifier.height(24.dp))
                                    // A button, not a link: it is the way out when the remembered database is gone
                                    SecondaryButton(
                                        text = stringResource(Res.string.entry_create_database),
                                        onClick = it,
                                        enabled = !shown.busy,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * The name: a link in the accent when it picks another database; [accent] - in the accent, not a link. While the
 * database opens the link and its hint grey out rather than go - what is under them does not jump (pass-docs,
 * Step 72, fix 4).
 */
@Composable
private fun DatabaseName(name: String, accent: Boolean, onClick: (() -> Unit)?, enabled: Boolean) {
    val colors = MaterialTheme.colorScheme
    val disabled = colors.onSurface.copy(alpha = DISABLED_ALPHA)
    Text(
        text = name,
        style = MaterialTheme.typography.headlineSmall,
        color = when {
            onClick != null && !enabled -> disabled
            onClick != null || accent -> colors.primary
            else -> colors.onBackground
        },
        textDecoration = if (onClick != null) TextDecoration.Underline else null,
        textAlign = TextAlign.Center,
        modifier = if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick) else Modifier,
    )
    if (onClick != null) {
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
private fun PasswordInput(
    entry: PasswordEntry,
    submitText: String,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisible: () -> Unit,
    onSubmit: () -> Unit,
) {
    val focus = remember { FocusRequester() }
    // The keyboard right away: typing the password is the one thing to do here
    LaunchedEffect(Unit) { focus.requestFocus() }

    PasswordField(
        value = entry.password,
        onValueChange = onPasswordChange,
        label = stringResource(Res.string.entry_password),
        visible = entry.passwordVisible,
        onToggleVisible = onTogglePasswordVisible,
        error = when (entry.problem) {
            EntryProblem.InvalidCredentials -> stringResource(Res.string.error_invalid_credentials)
            else -> null
        },
        enabled = !entry.busy,
        onImeAction = onSubmit,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focus),
    )
    Spacer(Modifier.height(16.dp))
    PrimaryButton(text = submitText, onClick = onSubmit, loading = entry.busy)
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
private fun BiometricSignInRow(entry: PasswordEntry, onBiometric: () -> Unit) {
    Reveal(visible = entry.biometricOn) {
        Column {
            Spacer(Modifier.height(12.dp))
            SecondaryButton(
                text = stringResource(Res.string.entry_biometric),
                onClick = onBiometric,
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
 * @param onRemove null where the key file is fixed - the locked screen: then there is no removing it, and
 * no attaching one to a database opened without
 */
@Composable
private fun KeyFileRow(entry: PasswordEntry, onAttach: () -> Unit, onRemove: (() -> Unit)?) {
    AnimatedContent(
        targetState = entry,
        contentKey = { it.keyFileUri != null },
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "key file",
    ) { shown ->
        if (shown.keyFileUri == null) {
            if (onRemove != null) {
                TextButton(onClick = onAttach, enabled = !shown.busy) {
                    Icon(imageVector = Icons.Outlined.Key, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(Res.string.entry_key_file_add))
                }
            }
        } else {
            AttachedKeyFile(entry = shown, onAttach = onAttach, onRemove = onRemove)
        }
    }
}

@Composable
private fun AttachedKeyFile(entry: PasswordEntry, onAttach: () -> Unit, onRemove: (() -> Unit)?) {
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
                TextButton(onClick = onAttach, enabled = !entry.busy) {
                    Text(stringResource(Res.string.action_pick_another_key_file))
                }
            }
        }
        onRemove?.let {
            TextButton(onClick = it, enabled = !entry.busy) {
                Text(stringResource(Res.string.entry_key_file_remove))
            }
        }
    }
}

@Composable
private fun FileProblem(
    problem: EntryProblem?,
    onRetry: (() -> Unit)?,
    onPickAnother: (() -> Unit)?,
    onCreateDatabase: (() -> Unit)?,
    busy: Boolean,
) {
    ErrorText(
        when (problem) {
            EntryProblem.UnsupportedFile -> stringResource(Res.string.error_unsupported_file)
            EntryProblem.CorruptedFile -> stringResource(Res.string.error_corrupted_file)
            else -> stringResource(Res.string.error_database_unreachable)
        },
    )
    Spacer(Modifier.height(24.dp))
    onRetry?.let {
        PrimaryButton(text = stringResource(Res.string.action_retry), onClick = it, loading = busy)
        Spacer(Modifier.height(12.dp))
    }
    onPickAnother?.let {
        SecondaryButton(text = stringResource(Res.string.action_pick_another_file), onClick = it, enabled = !busy)
    }
    onCreateDatabase?.let {
        Spacer(Modifier.height(12.dp))
        SecondaryButton(text = stringResource(Res.string.entry_create_database), onClick = it, enabled = !busy)
    }
}

@Composable
private fun ErrorText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
    )
}

/** The shield over the password: smaller than on the welcome screen, which has nothing else to show. */
private val SHIELD_SIZE = 96.dp

/** The shield alone while the state is read - as large as on the welcome screen. */
private val SHIELD_SIZE_READING = 140.dp

/** The shield shrinking and the rest opening out under it - one length for both, so they go as one. */
private const val READ_MILLIS = 400
