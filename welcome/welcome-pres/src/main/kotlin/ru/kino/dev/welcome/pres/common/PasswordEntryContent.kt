package ru.kino.dev.welcome.pres.common

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
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import ru.kino.dev.core.pres.R
import ru.kino.dev.ui.components.GradientBackground
import ru.kino.dev.ui.components.PasswordField
import ru.kino.dev.ui.components.PrimaryButton
import ru.kino.dev.ui.components.SecondaryButton

/**
 * The layout of entering a password, shared by the unlock and the locked screens - one layout, two
 * processors (pass-docs, `decisions.md`, "`unlock` и `locked` — два экрана"). No state of its own.
 *
 * @param databaseName the name of the database, shown as the title
 * @param onDatabaseClick null when the database cannot be changed here; otherwise the name is a link that
 * picks another file
 * @param onPickAnotherDatabase null when there is no other database to pick - the locked screen
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
    onRemoveKeyFile: () -> Unit,
    onDatabaseClick: (() -> Unit)?,
    onPickAnotherDatabase: (() -> Unit)?,
    subtitle: String? = null,
) {
    val problem = entry.problem

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
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp),
            )
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
            DatabaseName(name = databaseName, onClick = onDatabaseClick.takeUnless { entry.busy })
            Spacer(Modifier.height(32.dp))

            if (problem.hidesPassword) {
                FileProblem(
                    problem = problem,
                    onRetry = onSubmit.takeUnless { problem == EntryProblem.UnsupportedFile },
                    onPickAnother = onPickAnotherDatabase,
                    busy = entry.busy,
                )
            } else {
                PasswordInput(
                    entry = entry,
                    submitText = submitText,
                    onPasswordChange = onPasswordChange,
                    onTogglePasswordVisible = onTogglePasswordVisible,
                    onSubmit = onSubmit,
                )
                Spacer(Modifier.height(16.dp))
                KeyFileRow(
                    entry = entry,
                    onAttach = onAttachKeyFile,
                    onRemove = onRemoveKeyFile,
                )
            }
        }
    }
}

@Composable
private fun DatabaseName(name: String, onClick: (() -> Unit)?) {
    val colors = MaterialTheme.colorScheme
    Text(
        text = name,
        style = MaterialTheme.typography.headlineSmall,
        color = if (onClick != null) colors.primary else colors.onBackground,
        textDecoration = if (onClick != null) TextDecoration.Underline else null,
        textAlign = TextAlign.Center,
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
    )
    if (onClick != null) {
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.entry_change_database_hint),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
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
        label = stringResource(R.string.entry_password),
        visible = entry.passwordVisible,
        onToggleVisible = onTogglePasswordVisible,
        error = when (entry.problem) {
            EntryProblem.InvalidCredentials -> stringResource(R.string.error_invalid_credentials)
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
    if (entry.problem == EntryProblem.Unexpected) {
        Spacer(Modifier.height(8.dp))
        ErrorText(stringResource(R.string.error_unexpected))
    }
}

@Composable
private fun KeyFileRow(entry: PasswordEntry, onAttach: () -> Unit, onRemove: () -> Unit) {
    val problem = entry.problem

    if (entry.keyFileUri == null) {
        TextButton(onClick = onAttach, enabled = !entry.busy) {
            Icon(imageVector = Icons.Outlined.Key, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.entry_key_file_add))
        }
        return
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Outlined.Key,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = stringResource(
                R.string.entry_key_file_name,
                entry.keyFileName ?: stringResource(R.string.entry_key_file_unnamed),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (problem.isAboutKeyFile) {
        Spacer(Modifier.height(4.dp))
        ErrorText(
            when (problem) {
                is EntryProblem.KeyFileTooLarge ->
                    stringResource(R.string.error_key_file_too_large, problem.maxMebibytes.toInt())
                else -> stringResource(R.string.error_key_file_unreachable)
            },
        )
        TextButton(onClick = onAttach, enabled = !entry.busy) {
            Text(stringResource(R.string.action_pick_another_key_file))
        }
    }
    TextButton(onClick = onRemove, enabled = !entry.busy) {
        Text(stringResource(R.string.entry_key_file_remove))
    }
}

@Composable
private fun FileProblem(
    problem: EntryProblem?,
    onRetry: (() -> Unit)?,
    onPickAnother: (() -> Unit)?,
    busy: Boolean,
) {
    ErrorText(
        when (problem) {
            EntryProblem.UnsupportedFile -> stringResource(R.string.error_unsupported_file)
            EntryProblem.CorruptedFile -> stringResource(R.string.error_corrupted_file)
            else -> stringResource(R.string.error_database_unreachable)
        },
    )
    Spacer(Modifier.height(24.dp))
    onRetry?.let {
        PrimaryButton(text = stringResource(R.string.action_retry), onClick = it, loading = busy)
        Spacer(Modifier.height(12.dp))
    }
    onPickAnother?.let {
        SecondaryButton(text = stringResource(R.string.action_pick_another_file), onClick = it, enabled = !busy)
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
