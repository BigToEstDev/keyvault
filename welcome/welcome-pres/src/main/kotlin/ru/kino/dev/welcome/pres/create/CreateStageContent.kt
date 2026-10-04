package ru.kino.dev.welcome.pres.create

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import ru.kino.dev.core.pres.R
import ru.kino.dev.ui.components.ConfirmDialog
import ru.kino.dev.welcome.PickedFile
import ru.kino.dev.welcome.pres.common.EntryProblem

/** The question before writing over a file that is not empty - for the key file, it may lock another database. */
@Composable
internal fun OverwriteDialog(overwrite: Overwrite, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    ConfirmDialog(
        title = stringResource(R.string.overwrite_title),
        text = stringResource(
            when (overwrite.file) {
                PickedFile.Database -> R.string.overwrite_database_text
                PickedFile.KeyFile -> R.string.overwrite_key_file_text
            },
        ),
        confirmText = stringResource(R.string.overwrite_confirm),
        dismissText = stringResource(R.string.action_cancel),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        destructive = true,
    )
}

/**
 * What went wrong while creating. Only a file that cannot be reached is likely here - the user picks
 * another one with the same button; anything else is ours.
 */
@Composable
internal fun CreateProblemText(problem: EntryProblem) {
    Text(
        text = stringResource(
            when (problem) {
                EntryProblem.Unreachable(PickedFile.Database) -> R.string.error_database_unreachable
                EntryProblem.Unreachable(PickedFile.KeyFile) -> R.string.error_key_file_unreachable
                else -> R.string.error_unexpected
            },
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
    )
}
