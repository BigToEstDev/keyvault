package ru.kino.dev.welcome.pres.biometricsetup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import ru.kino.dev.ui.biometric.BiometricPurpose
import ru.kino.dev.ui.components.BackHandler
import ru.kino.dev.welcome.pres.common.rememberBiometricPrompt
import ru.kino.dev.welcome.pres.create.CreateDraft

@Composable
internal fun BiometricSetupScreen(
    dbKey: String,
    draft: CreateDraft,
    processor: BiometricSetupProcessor = koinViewModel { parametersOf(dbKey, draft) },
) {
    val state by processor.collectAsState()
    val showPrompt = rememberBiometricPrompt { processor.onPromptFinished(it) }
    BackHandler(enabled = !state.busy, onBack = processor::onSkip)

    processor.collectSideEffect { effect ->
        when (effect) {
            is BiometricSetupSideEffect.ShowPrompt -> showPrompt(effect.session, BiometricPurpose.Enable)
        }
    }

    processor.BiometricSetupContent(state)
}
