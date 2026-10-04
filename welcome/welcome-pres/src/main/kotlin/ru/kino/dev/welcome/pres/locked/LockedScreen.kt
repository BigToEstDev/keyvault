package ru.kino.dev.welcome.pres.locked

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import ru.kino.dev.core.pres.R
import ru.kino.dev.welcome.pres.common.PasswordEntryContent
import ru.kino.dev.welcome.pres.common.rememberOpenDocument

@Composable
internal fun LockedScreen(
    dbKey: String,
    processor: LockedProcessor = hiltViewModel<LockedProcessor, LockedProcessor.Factory>(
        creationCallback = { factory -> factory.create(dbKey) },
    ),
) {
    val state by processor.collectAsState()
    val pickKeyFile = rememberOpenDocument { processor.action(LockedAction.KeyFilePicked(it)) }
    val activity = LocalActivity.current

    // "Back" puts the app away: going back would show `vault` under the lock, and one button would undo it
    BackHandler { activity?.moveTaskToBack(true) }

    processor.collectSideEffect { effect ->
        when (effect) {
            LockedSideEffect.PickKeyFile -> pickKeyFile()
        }
    }

    PasswordEntryContent(
        databaseName = state.databaseName ?: stringResource(R.string.entry_database_fallback),
        subtitle = stringResource(R.string.entry_locked_title),
        entry = state.entry,
        submitText = stringResource(R.string.entry_unlock),
        onPasswordChange = { processor.action(LockedAction.PasswordChanged(it)) },
        onTogglePasswordVisible = { processor.action(LockedAction.TogglePasswordVisible) },
        onSubmit = { processor.action(LockedAction.Submit) },
        onAttachKeyFile = { processor.action(LockedAction.AttachKeyFile) },
        onRemoveKeyFile = { processor.action(LockedAction.RemoveKeyFile) },
        onDatabaseClick = null,
        onPickAnotherDatabase = null,
    )
}
