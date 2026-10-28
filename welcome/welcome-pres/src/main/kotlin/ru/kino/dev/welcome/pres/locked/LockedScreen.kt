package ru.kino.dev.welcome.pres.locked

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import ru.kino.dev.core.pres.*
import ru.kino.dev.ui.biometric.BiometricPurpose
import ru.kino.dev.ui.files.rememberOpenDocument
import ru.kino.dev.ui.res.stringResource
import ru.kino.dev.welcome.pres.common.PasswordEntryContent
import ru.kino.dev.welcome.pres.common.rememberBiometricPrompt

@Composable
internal fun LockedScreen(
    dbKey: String,
    processor: LockedProcessor = koinViewModel { parametersOf(dbKey) },
) {
    val state by processor.collectAsState()
    val pickKeyFile = rememberOpenDocument { processor.action(LockedAction.KeyFilePicked(it)) }
    val showBiometricPrompt = rememberBiometricPrompt { processor.action(LockedAction.BiometricFinished(it)) }
    val activity = LocalActivity.current

    // "Back" puts the app away: going back would show `vault` under the lock, and one button would undo it
    BackHandler { activity?.moveTaskToBack(true) }

    processor.collectSideEffect { effect ->
        when (effect) {
            LockedSideEffect.PickKeyFile -> pickKeyFile()
            is LockedSideEffect.ShowBiometricPrompt -> showBiometricPrompt(effect.session, BiometricPurpose.Unlock)
        }
    }

    PasswordEntryContent(
        databaseName = state.databaseName ?: stringResource(Res.string.entry_database_fallback),
        subtitle = stringResource(Res.string.entry_locked_title),
        entry = state.entry,
        submitText = stringResource(Res.string.entry_unlock),
        onPasswordChange = { processor.action(LockedAction.PasswordChanged(it)) },
        onTogglePasswordVisible = { processor.action(LockedAction.TogglePasswordVisible) },
        onSubmit = { processor.action(LockedAction.Submit) },
        onAttachKeyFile = { processor.action(LockedAction.AttachKeyFile) },
        onRemoveKeyFile = null,
        onBiometric = { processor.action(LockedAction.Biometric) },
        onRetry = { processor.action(LockedAction.Submit) },
        onCreateDatabase = null,
        onDatabaseClick = null,
        onPickAnotherDatabase = null,
        read = state.read,
        locked = true,
    )
}
