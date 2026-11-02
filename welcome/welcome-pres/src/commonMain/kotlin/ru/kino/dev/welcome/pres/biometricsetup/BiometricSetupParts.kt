package ru.kino.dev.welcome.pres.biometricsetup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import ru.kino.dev.ui.components.PrimaryButton
import ru.kino.dev.ui.components.SecondaryButton
import ru.kino.dev.ui.res.stringResource

// The pieces of the layouts of offering biometrics (pass-docs, Step 95): the layouts place them

/** What biometrics give, and turning them on or skipping - centred. */
@Composable
internal fun BiometricSetupActions.BiometricSetupForm(state: BiometricSetupState) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
