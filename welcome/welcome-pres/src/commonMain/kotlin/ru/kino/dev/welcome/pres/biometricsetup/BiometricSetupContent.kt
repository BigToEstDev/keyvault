package ru.kino.dev.welcome.pres.biometricsetup

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.kino.dev.core.pres.*
import ru.kino.dev.ui.components.GradientBackground
import ru.kino.dev.ui.components.PrimaryButton
import ru.kino.dev.ui.components.SecondaryButton
import ru.kino.dev.ui.res.stringResource

/** Offering biometrics after creating, in one column (pass-docs, `docs/app/mvi-arc.md`). */
@Composable
internal fun BiometricSetupActions.BiometricSetupContent(state: BiometricSetupState) {
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
                text = stringResource(Res.string.biometric_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.biometric_text),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(48.dp))
            if (state.failed) {
                Text(
                    text = stringResource(Res.string.biometric_enable_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
            }
            PrimaryButton(
                text = stringResource(Res.string.biometric_enable),
                onClick = ::onEnable,
                loading = state.busy,
            )
            Spacer(Modifier.height(12.dp))
            SecondaryButton(text = stringResource(Res.string.biometric_skip), onClick = ::onSkip, enabled = !state.busy)
        }
    }
}
