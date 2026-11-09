package ru.kino.dev.welcome.pres.create

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextAlign
import ru.kino.dev.core.pres.*
import ru.kino.dev.ui.components.ConfirmDialog
import ru.kino.dev.ui.components.MessageDialog
import ru.kino.dev.ui.res.stringResource
import ru.kino.dev.welcome.PickedFile
import ru.kino.dev.welcome.pres.common.EntryProblem

/** The question before writing over a file that is not empty - for the key file, it may lock another database. */
@Composable
internal fun OverwriteDialog(overwrite: Overwrite, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    ConfirmDialog(
        title = stringResource(Res.string.overwrite_title),
        text = stringResource(
            when (overwrite.file) {
                PickedFile.Database -> Res.string.overwrite_database_text
                PickedFile.KeyFile -> Res.string.overwrite_key_file_text
            },
        ),
        confirmText = stringResource(Res.string.overwrite_confirm),
        dismissText = stringResource(Res.string.action_cancel),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        destructive = true,
    )
}

/** Creating was cut off by the killed process: what was typed is gone, start over (pass-docs, Step 112). */
@Composable
internal fun InterruptedDialog(onDismiss: () -> Unit) {
    MessageDialog(
        title = stringResource(Res.string.create_interrupted_title),
        text = stringResource(Res.string.create_interrupted_text),
        onDismiss = onDismiss,
    )
}

/**
 * What went wrong while creating: a file that cannot be reached, or an existing file picked as the key file
 * that is too large - the user picks another with the same buttons; anything else is ours.
 */
@Composable
internal fun CreateProblemText(problem: EntryProblem) {
    Text(
        text = when (problem) {
            EntryProblem.Unreachable(PickedFile.Database) -> stringResource(Res.string.error_database_unreachable)
            EntryProblem.Unreachable(PickedFile.KeyFile) -> stringResource(Res.string.error_key_file_unreachable)
            is EntryProblem.KeyFileTooLarge ->
                stringResource(Res.string.error_key_file_too_large, problem.maxMebibytes.toInt())
            else -> stringResource(Res.string.error_unexpected)
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
    )
}
