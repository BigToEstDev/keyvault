package ru.kino.dev.welcome.pres.biometricsetup

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import ru.kino.dev.core.pres.R
import ru.kino.dev.ui.components.GradientBackground
import ru.kino.dev.ui.components.PrimaryButton
import ru.kino.dev.ui.components.SecondaryButton
import ru.kino.dev.welcome.pres.common.BiometricPurpose
import ru.kino.dev.welcome.pres.common.rememberBiometricPrompt
import ru.kino.dev.welcome.pres.create.CreateDraft

@Composable
internal fun BiometricSetupScreen(
    dbKey: String,
    draft: CreateDraft,
    processor: BiometricSetupProcessor = hiltViewModel<BiometricSetupProcessor, BiometricSetupProcessor.Factory>(
        creationCallback = { factory -> factory.create(dbKey, draft) },
    ),
) {
    val state by processor.collectAsState()
    val showPrompt = rememberBiometricPrompt { processor.action(BiometricSetupAction.PromptFinished(it)) }
    val skip = { processor.action(BiometricSetupAction.Skip) }
    BackHandler(enabled = !state.busy) { skip() }

    processor.collectSideEffect { effect ->
        when (effect) {
            is BiometricSetupSideEffect.ShowPrompt -> showPrompt(effect.session, BiometricPurpose.Enable)
        }
    }

    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Fingerprint,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp),
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.biometric_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.biometric_text),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(48.dp))
            if (state.failed) {
                Text(
                    text = stringResource(R.string.biometric_enable_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
            }
            PrimaryButton(
                text = stringResource(R.string.biometric_enable),
                onClick = { processor.action(BiometricSetupAction.Enable) },
                loading = state.busy,
            )
            Spacer(Modifier.height(12.dp))
            SecondaryButton(text = stringResource(R.string.biometric_skip), onClick = { skip() }, enabled = !state.busy)
        }
    }
}
