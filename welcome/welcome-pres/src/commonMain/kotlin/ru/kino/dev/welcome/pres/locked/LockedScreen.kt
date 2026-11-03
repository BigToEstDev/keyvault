package ru.kino.dev.welcome.pres.locked

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import ru.kino.dev.core.pres.*
import ru.kino.dev.pane.koinProcessor
import ru.kino.dev.ui.biometric.BiometricPurpose
import ru.kino.dev.ui.components.BackHandler
import ru.kino.dev.ui.components.rememberPutAppAway
import ru.kino.dev.ui.files.DocumentKind
import ru.kino.dev.ui.files.rememberOpenDocument
import ru.kino.dev.ui.res.stringResource
import ru.kino.dev.welcome.pres.common.passwordentry.PasswordEntryAbilities
import ru.kino.dev.welcome.pres.common.passwordentry.PasswordEntryContent
import ru.kino.dev.welcome.pres.common.rememberBiometricPrompt

@Composable
internal fun LockedScreen(
    dbKey: String,
    processor: LockedProcessor = koinProcessor(dbKey),
) {
    val state by processor.collectAsState()
    val pickKeyFile = rememberOpenDocument(DocumentKind.KeyFile) { processor.onKeyFilePicked(it) }
    val showBiometricPrompt = rememberBiometricPrompt { processor.onBiometricFinished(it) }
    val putAppAway = rememberPutAppAway()

    // "Back" puts the app away: going back would show `vault` under the lock, and one button would undo it
    BackHandler { putAppAway() }

    processor.collectSideEffect { effect ->
        when (effect) {
            LockedSideEffect.PickKeyFile -> pickKeyFile()
            is LockedSideEffect.ShowBiometricPrompt -> showBiometricPrompt(effect.session, BiometricPurpose.Unlock)
        }
    }

    processor.PasswordEntryContent(
        databaseName = state.databaseName ?: stringResource(Res.string.entry_database_fallback),
        subtitle = stringResource(Res.string.entry_locked_title),
        entry = state.entry,
        submitText = stringResource(Res.string.entry_unlock),
        abilities = PasswordEntryAbilities.Locked,
        read = state.read,
    )
}
