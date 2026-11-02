package ru.kino.dev.welcome.pres.unlock

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import ru.kino.dev.core.pres.*
import ru.kino.dev.ui.biometric.BiometricPurpose
import ru.kino.dev.ui.files.DocumentKind
import ru.kino.dev.ui.files.rememberOpenDocument
import ru.kino.dev.ui.res.stringResource
import ru.kino.dev.welcome.pres.common.passwordentry.PasswordEntryAbilities
import ru.kino.dev.welcome.pres.common.passwordentry.PasswordEntryContent
import ru.kino.dev.welcome.pres.common.rememberBiometricPrompt

/** "Back" is the default here: out of the app on start, back to the welcome screen after picking a file. */
@Composable
internal fun UnlockScreen(
    pickedUri: String?,
    processor: UnlockProcessor = koinViewModel { parametersOf(pickedUri) },
) {
    val state by processor.collectAsState()
    val pickDatabase = rememberOpenDocument(DocumentKind.Database) { processor.onDatabasePicked(it) }
    val pickKeyFile = rememberOpenDocument(DocumentKind.KeyFile) { processor.onKeyFilePicked(it) }
    val showBiometricPrompt = rememberBiometricPrompt { processor.onBiometricFinished(it) }

    processor.collectSideEffect { effect ->
        when (effect) {
            UnlockSideEffect.PickDatabase -> pickDatabase()
            UnlockSideEffect.PickKeyFile -> pickKeyFile()
            is UnlockSideEffect.ShowBiometricPrompt -> showBiometricPrompt(effect.session, BiometricPurpose.Unlock)
        }
    }

    processor.PasswordEntryContent(
        databaseName = state.title ?: stringResource(Res.string.entry_database_fallback),
        entry = state.entry,
        submitText = stringResource(Res.string.entry_open),
        abilities = PasswordEntryAbilities.Unlock,
        read = state.read,
    )
}
