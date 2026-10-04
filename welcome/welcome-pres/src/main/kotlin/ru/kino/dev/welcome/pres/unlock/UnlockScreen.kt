package ru.kino.dev.welcome.pres.unlock

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import ru.kino.dev.core.pres.R
import ru.kino.dev.welcome.pres.common.PasswordEntryContent
import ru.kino.dev.welcome.pres.common.rememberOpenDocument

/** "Back" is the default here: out of the app on start, back to the welcome screen after picking a file. */
@Composable
internal fun UnlockScreen(
    pickedUri: String?,
    processor: UnlockProcessor = hiltViewModel<UnlockProcessor, UnlockProcessor.Factory>(
        creationCallback = { factory -> factory.create(pickedUri) },
    ),
) {
    val state by processor.collectAsState()
    val pickDatabase = rememberOpenDocument { processor.action(UnlockAction.DatabasePicked(it)) }
    val pickKeyFile = rememberOpenDocument { processor.action(UnlockAction.KeyFilePicked(it)) }

    processor.collectSideEffect { effect ->
        when (effect) {
            UnlockSideEffect.PickDatabase -> pickDatabase()
            UnlockSideEffect.PickKeyFile -> pickKeyFile()
        }
    }

    val changeDatabase = { processor.action(UnlockAction.ChangeDatabase) }
    PasswordEntryContent(
        databaseName = state.title ?: stringResource(R.string.entry_database_fallback),
        entry = state.entry,
        submitText = stringResource(R.string.entry_open),
        onPasswordChange = { processor.action(UnlockAction.PasswordChanged(it)) },
        onTogglePasswordVisible = { processor.action(UnlockAction.TogglePasswordVisible) },
        onSubmit = { processor.action(UnlockAction.Submit) },
        onAttachKeyFile = { processor.action(UnlockAction.AttachKeyFile) },
        onRemoveKeyFile = { processor.action(UnlockAction.RemoveKeyFile) },
        onDatabaseClick = { changeDatabase() },
        onPickAnotherDatabase = { changeDatabase() },
    )
}
